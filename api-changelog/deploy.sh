./gradlew clean
./gradlew build

REGION=asia-east1
gcloud functions deploy api-changelog \
    --gen2 \
    --memory 256MB \
    --region ${REGION} \
    --runtime java11 \
    --entry-point functions.App \
    --source build/libs \
    --trigger-http \
    --timeout 180s \
    --set-secrets "CLICKUP_API_TOKEN=CLICKUP_API_TOKEN:latest" \
    --set-secrets "GITHUB_DATA=GITHUB_DATA:latest" \
    --set-secrets "SLACK_API_AUTHORIZATION=SLACK_API_AUTHORIZATION:latest" \
    --set-secrets "SLACK_WEBHOOKS=SLACK_WEBHOOKS:latest"

REPOSITORY=gcf-artifacts
PACKAGE=api--changelog
for VERSION in $(gcloud artifacts versions list --repository=${REPOSITORY} --location=${REGION} --package=${PACKAGE} --format='value(name)'); do
    gcloud artifacts versions delete ${VERSION} --quiet --repository=${REPOSITORY} --location=${REGION} --package=${PACKAGE}
done