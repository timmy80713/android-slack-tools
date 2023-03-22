rm -rf .idea/

REGION=asia-east1
gcloud functions deploy slack-tools \
    --gen2 \
    --memory 256MB \
    --region ${REGION} \
    --runtime nodejs18 \
    --entry-point useTool \
    --source . \
    --trigger-http \
    --allow-unauthenticated \
    --timeout 180s

REPOSITORY=gcf-artifacts
PACKAGE=slack--tools
for VERSION in $(gcloud artifacts versions list --repository=${REPOSITORY} --location=${REGION} --package=${PACKAGE} --format='value(name)'); do
    gcloud artifacts versions delete ${VERSION} --quiet --repository=${REPOSITORY} --location=${REGION} --package=${PACKAGE}
done
