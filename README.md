# Trakt Ratings Remover

Script to remove all your ratings from Trakt.

I joined Trakt when they only had thumbs up/down ratings. Eventually they migrated to a
1 - 10 system, making a thumbs up valued at 10 and a thumbs down valued at 1. Thus, my account
was filled with an overwhelming and inaccurate set of 1-rated and 10-rated content.

This very quick script will list them all, authenticate to your user, and then delete them all.
Enjoy your clean slate.


## Usage

1. Create an app to get a client ID and client secret at https://trakt.tv/oauth/applications/new
2. Build this app with `./gradlew assemble`
3. List your ratings

    ```
    $ ./build/install/trakt-ratings-remover/bin/trakt-ratings-remover \
          --client-id <your-client-id-here> \
          --client-secret <your-client-secret-here> \
          YourUserName
    ```

4. Delete your ratings

    ```
   $ ./build/install/trakt-ratings-remover/bin/trakt-ratings-remover \
         --client-id <your-client-id-here> \
         --client-secret <your-client-secret-here> \
         --delete \
         YourUserName
   ```

   You will be prompted to visit Trakt and enter a code after which deletion will immediately occur.



# License

    Copyright 2024 Jake Wharton

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
