To get a fresh Refresh Token (which lasts indefinitely and generates 
short-lived access tokens), you usually perform a one-time manual step in 
your browser:

    1.  Construct this URL (replace YOUR_APP_KEY with your actual App Key from the Settings tab):
    Plaintext

    https://www.dropbox.com/oauth2/authorize?client_id=YOUR_APP_KEY&token_access_type=offline&response_type=code&scope=files.content.read%20files.content.write

    Note the %20 between the two scopes.

    2.  Paste that into your browser. You’ll see the standard Dropbox approval screen.

    3.  Click Allow. You will be given a code.

    4.  Exchange that code for a refresh token using curl or a tool like Postman:
    Bash

    curl https://api.dropbox.com/oauth2/token \
        -d code=YOUR_CODE_FROM_STEP_3 \
        -d grant_type=authorization_code \
        -u YOUR_APP_KEY:YOUR_APP_SECRET

    5.  The JSON response will contain a refresh_token. Store that in your app. 
    Your DropboxService can then use that to get a fresh access_token whenever 
    it needs to perform a write or delete.

The most recent curl command is:

https://www.dropbox.com/oauth2/authorize?client_id=biuz2x4l15x07ce&token_access_type=offline&response_type=code&scope=files.content.read%20files.content.write
curl https://api.dropbox.com/oauth2/token -d code=rnOCTvRS_s0AAAAAAABgnsk4Dkh8ZEG4zUgTBcW68Fc -d grant_type=authorization_code -u biuz2x4l15x07ce:fdbfex317e38zwg
