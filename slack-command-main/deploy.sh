rm -rf .idea/

REGION=asia-east1
gcloud functions deploy slack-command-main \
    --gen2 \
    --memory 256MB \
    --region ${REGION} \
    --runtime nodejs18 \
    --entry-point main \
    --source . \
    --trigger-http \
    --allow-unauthenticated \
    --timeout 180s \
    --set-secrets "SLACK_SIGNING_SECRET=SLACK_SIGNING_SECRET:latest"

REPOSITORY=gcf-artifacts
PACKAGE=slack--command--main
for VERSION in $(gcloud artifacts versions list --repository=${REPOSITORY} --location=${REGION} --package=${PACKAGE} --format='value(name)'); do
    gcloud artifacts versions delete ${VERSION} --quiet --repository=${REPOSITORY} --location=${REGION} --package=${PACKAGE}
done
