./gradlew clean
./gradlew buildFunction
# https://cloud.google.com/functions/docs/deploy
gcloud functions deploy slack-tools \
    --gen2 \
    --memory=512MB \
    --runtime=java11 \
    --region=us-central1 \
    --entry-point=functions.App \
    --source=build/deploy \
    --trigger-http \
    --allow-unauthenticated \
    --set-env-vars "GOOGLE_CLOUD_PROJECT_ID=android-slack-tools" \
    --set-secrets "SLACK_SIGNING_SECRET=SLACK_SIGNING_SECRET:latest"