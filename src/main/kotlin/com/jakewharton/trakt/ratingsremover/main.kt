@file:JvmName("Main")

package com.jakewharton.trakt.ratingsremover

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.switch
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.BASIC
import retrofit2.HttpException
import retrofit2.Invocation
import retrofit2.Retrofit
import retrofit2.create

fun main(vararg args: String) = MainCommand().main(args)

private class MainCommand : CliktCommand(
	name = "trakt-ratings-remover",
	help = "Lists and optionally removes all ratings from Trakt",
) {
	private val debug by option(hidden = true)
		.switch<Debug>(mapOf("--debug" to Debug.Console))
		.default(Debug.Disabled)

	private val clientId by option("--client-id", metavar = "id")
		.help("OAuth client ID from Trakt app")
		.required()

	private val clientSecret by option("--client-secret", metavar = "secret")
		.help("OAuth client secret from Trakt app")
		.required()

	private val delete by option("--delete")
		.help("Perform deletion of all discovered ratings")
		.flag()

	private val username by argument()
		.help("Username of Trakt account")

	private val client by lazy {
		OkHttpClient.Builder()
			.addInterceptor { chain ->
				val request = chain.request()
				val requestBuilder = request.newBuilder()
					.addHeader("trakt-api-version", "2")

				val method = request.tag(Invocation::class.java)!!.method()
				if (!method.isAnnotationPresent(Unauthenticated::class.java)) {
					requestBuilder.addHeader("trakt-api-key", clientId)
				}

				chain.proceed(requestBuilder.build())
			}
			.addNetworkInterceptor(HttpLoggingInterceptor(debug::log).setLevel(BASIC))
			.build()
	}

	private val json = Json {
		ignoreUnknownKeys = true
	}

	private val api by lazy {
		Retrofit.Builder()
			.baseUrl("https://api.trakt.tv/")
			.addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
			.client(client)
			.build()
			.create<TraktApi>()
	}

	override fun run() {
		try {
			runBlocking {
				val ratings = listRatings()
				printRatings(ratings)
				println()

				if (delete) {
					val authorization = authenticate()
					deleteRatings(ratings, authorization)
				} else {
					println("Run again with --delete to remove these ratings.")
				}
			}
		} finally {
			client.apply {
				dispatcher.executorService.shutdown()
				connectionPool.evictAll()
			}
		}
	}

	private suspend fun listRatings(): List<RatedMedia> {
		val response = api.getRatings(username)
		when (response.code()) {
			200 -> {
				check(response.headers()["X-Pagination-Page"] == null) {
					"Unexpected pagination header present"
				}
				return response.body()!!
			}

			else -> throw HttpException(response)
		}
	}

	private fun printRatings(ratings: List<RatedMedia>) {
		println("Found ${ratings.size} total ratings")
		println()
		for (rating in ratings) {
			println(
				when (rating) {
					is RatedMovie -> "MOVIE ${rating.rating} ${rating.movie.title}"
					is RatedShow -> "SHOW ${rating.rating} ${rating.show.title}"
					is RatedSeason -> "SEASON ${rating.rating} ${rating.show.title} S${rating.season.number}"
					is RatedEpisode -> "EPISODE ${rating.rating} ${rating.show.title} S${rating.episode.season}E${rating.episode.number}"
				},
			)
		}
	}

	private suspend fun authenticate(): String {
		val codeResponse = api.generateDeviceCode(
			GenerateDeviceCodeRequest(
				client_id = clientId,
			),
		)
		println("Visit ${codeResponse.verification_url} and enter ${codeResponse.user_code} to authenticate.")
		println()
		print("Checking every ${codeResponse.interval.seconds} for ${codeResponse.expires_in.seconds}…")
		for (i in 0 until codeResponse.expires_in step codeResponse.interval) {
			delay(codeResponse.interval.seconds)

			val pollResponse = api.pollAccessToken(
				PollAccessTokenRequest(
					code = codeResponse.device_code,
					client_id = clientId,
					client_secret = clientSecret,
				),
			)
			when (pollResponse.code()) {
				200 -> {
					println(" Success!")
					println()

					val body = pollResponse.body()!!
					check(body.token_type == "bearer") {
						"Unknown token type: ${body.token_type}"
					}
					return "Bearer ${body.access_token}"
				}
				400 -> print("…")
				410 -> break
				else -> {
					println()
					throw HttpException(pollResponse)
				}
			}
		}

		println(" Timed out!")
		exitProcess(1)
	}

	private suspend fun deleteRatings(ratings: List<RatedMedia>, authorization: String) {
		println("Deleting ${ratings.size} ratings…")
		api.removeRatings(
			authorization = authorization,
			request = RemoveRatingsRequest(
				movies = ratings.mapNotNull { (it as? RatedMovie)?.movie },
				shows = ratings.mapNotNull { (it as? RatedShow)?.show },
				seasons = ratings.mapNotNull { (it as? RatedSeason)?.season },
				episodes = ratings.mapNotNull { (it as? RatedEpisode)?.episode },
			),
		)
		println(" Done!")
	}
}
