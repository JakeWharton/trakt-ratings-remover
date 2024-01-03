package com.jakewharton.trakt.ratingsremover

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

internal annotation class Unauthenticated

internal interface TraktApi {
	/* https://trakt.docs.apiary.io/#reference/authentication-devices/device-code/generate-new-device-codes */
	@Unauthenticated
	@POST("oauth/device/code")
	suspend fun generateDeviceCode(
		@Body request: GenerateDeviceCodeRequest,
	): GenerateDeviceCodeResponse

	/* https://trakt.docs.apiary.io/#reference/authentication-devices/get-token/poll-for-the-access_token */
	@Unauthenticated
	@POST("oauth/device/token")
	suspend fun pollAccessToken(
		@Body request: PollAccessTokenRequest,
	): Response<PollAccessTokenResponse>

	/* https://trakt.docs.apiary.io/#reference/users/ratings/get-ratings */
	@GET("users/{id}/ratings")
	suspend fun getRatings(
		@Path("id") id: String,
	): Response<List<RatedMedia>>

	/* https://trakt.docs.apiary.io/#reference/sync/remove-ratings/remove-ratings */
	@POST("sync/ratings/remove")
	suspend fun removeRatings(
		@Header("Authorization") authorization: String,
		@Body request: RemoveRatingsRequest,
	)
}

@Serializable
internal data class GenerateDeviceCodeRequest(
	val client_id: String,
)

@Serializable
internal data class GenerateDeviceCodeResponse(
	val device_code: String,
	val user_code: String,
	val verification_url: String,
	val expires_in: Int,
	val interval: Int,
)

@Serializable
internal data class PollAccessTokenRequest(
	val code: String,
	val client_id: String,
	val client_secret: String,
)

@Serializable
internal data class PollAccessTokenResponse(
	val access_token: String,
	val token_type: String,
)

@Serializable
internal data class RemoveRatingsRequest(
	val movies: List<Movie> = emptyList(),
	val shows: List<Show> = emptyList(),
	val seasons: List<Season> = emptyList(),
	val episodes: List<Episode> = emptyList(),
)

@Serializable
internal sealed interface RatedMedia {
	val rating: Int
}

@Serializable
@SerialName("movie")
internal data class RatedMovie(
	override val rating: Int,
	val movie: Movie,
) : RatedMedia

@Serializable
@SerialName("show")
internal data class RatedShow(
	override val rating: Int,
	val show: Show,
) : RatedMedia

@Serializable
@SerialName("season")
internal data class RatedSeason(
	override val rating: Int,
	val show: Show,
	val season: Season,
) : RatedMedia

@Serializable
@SerialName("episode")
internal data class RatedEpisode(
	override val rating: Int,
	val show: Show,
	val episode: Episode,
) : RatedMedia

@Serializable
internal data class Movie(
	val title: String,
	val ids: Ids,
)

@Serializable
internal data class Show(
	val title: String,
	val ids: Ids,
)

@Serializable
internal data class Season(
	val number: Int,
)

@Serializable
internal data class Episode(
	val season: Int,
	val number: Int,
)

@Serializable
internal data class Ids(
	val trakt: Long,
)
