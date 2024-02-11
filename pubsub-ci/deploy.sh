./gradlew clean
./gradlew build

REGION=asia-east1
gcloud functions deploy pubsub-ci \
    --gen2 \
    --memory 256MB \
    --region ${REGION} \
    --runtime java11 \
    --entry-point functions.App \
    --source build/libs \
    --trigger-topic "slack-command.ci" \
    --timeout 180s \
    --set-secrets "BITRISE_BUILD_TRIGGER_TOKEN=BITRISE_BUILD_TRIGGER_TOKEN:latest" \
    --set-secrets "SLACK_WEBHOOKS=SLACK_WEBHOOKS:latest"

REPOSITORY=gcf-artifacts
PACKAGE=pubsub--ci
for VERSION in $(gcloud artifacts versions list --repository=${REPOSITORY} --location=${REGION} --package=${PACKAGE} --format='value(name)'); do
    gcloud artifacts versions delete ${VERSION} --quiet --repository=${REPOSITORY} --location=${REGION} --package=${PACKAGE}
done