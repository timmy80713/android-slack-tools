./gradlew clean
./gradlew build

REGION=asia-east1
gcloud functions deploy slack-tools-ci \
    --gen2 \
    --memory 256MB \
    --region ${REGION} \
    --runtime java11 \
    --entry-point functions.App \
    --source build/libs \
    --trigger-topic "slack-tools.ci" \
    --timeout 180s \
    --set-secrets "BITRISE_BUILD_TRIGGER_TOKEN=BITRISE_BUILD_TRIGGER_TOKEN:latest"

REPOSITORY=gcf-artifacts
PACKAGE=slack--tools--ci
for VERSION in $(gcloud artifacts versions list --repository=${REPOSITORY} --location=${REGION} --package=${PACKAGE} --format='value(name)'); do
    gcloud artifacts versions delete ${VERSION} --quiet --repository=${REPOSITORY} --location=${REGION} --package=${PACKAGE}
done