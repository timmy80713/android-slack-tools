./gradlew clean
./gradlew buildFunction
gcloud functions deploy slack-tool-build-by-circleci \
    --gen2 \
    --memory=512MB \
    --runtime=java11 \
    --region=us-central1 \
    --entry-point=functions.App \
    --source=build/deploy \
    --trigger-topic "slack-tools.build-app" \
    --set-secrets "CIRCLECI_PERSONAL_API_TOKEN=CIRCLECI_PERSONAL_API_TOKEN:latest"