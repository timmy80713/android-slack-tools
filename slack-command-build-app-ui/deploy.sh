rm -rf .idea/

REGION=asia-east1
gcloud functions deploy slack-command-build-app-ui \
    --gen2 \
    --memory 16GiB \
    --region ${REGION} \
    --runtime nodejs20 \
    --entry-point main \
    --source . \
    --trigger-http \
    --allow-unauthenticated \
    --timeout 180s \
    --set-secrets "GITHUB_DATA=GITHUB_DATA:latest" \
    --set-secrets "GITHUB_PERSONAL_ACCESS_TOKEN=GITHUB_PERSONAL_ACCESS_TOKEN:latest" \
    --set-secrets "SLACK_SIGNING_SECRET=SLACK_SIGNING_SECRET:latest"

REPOSITORY=gcf-artifacts
PACKAGE=android--slack--tools__asia--east1__slack--command--build--app--ui
for VERSION in $(gcloud artifacts versions list --repository=${REPOSITORY} --location=${REGION} --package=${PACKAGE} --format='value(name)'); do
    gcloud artifacts versions delete ${VERSION} --quiet --repository=${REPOSITORY} --location=${REGION} --package=${PACKAGE}
done
