#!/bin/sh
docker run --privileged --rm tonistiigi/binfmt --install all
export DOCKER_DEFAULT_PLATFORM=linux/amd64
# Function to cleanup temporary files
cleanup() {
    echo "Cleaning up temporary files..."
    rm -f "image.tar"
    rm -f "pulsar-python-mapper-ms.zip"
    echo "Cleanup completed."
}

# Set trap to cleanup on script exit (success or failure)
trap cleanup EXIT

echo "Starting deployment process..."

# Build the Docker image (use legacy builder if buildx is causing issues)
if command -v docker buildx >/dev/null 2>&1 && docker buildx version >/dev/null 2>&1; then
    echo "Using Docker BuildX"
    docker buildx build --platform=linux/amd64  -t pulsar-python-mapper-ms .
else
    echo "Using legacy Docker build"
    docker build -t pulsar-python-mapper-ms .
fi

# Check if build was successful
if [ $? -ne 0 ]; then
    echo "ERROR: Docker build failed"
    exit 1
fi

# Save the image as tar
echo "Saving Docker image..."
docker save pulsar-python-mapper-ms > "image.tar"

# Check if save was successful
if [ $? -ne 0 ]; then
    echo "ERROR: Docker save failed"
    exit 1
fi

# Create zip file with cumulocity.json and image
echo "Creating deployment package..."
zip pulsar-python-mapper-ms cumulocity.json image.tar

# Check if zip was successful
if [ $? -ne 0 ]; then
    echo "ERROR: Zip creation failed"
    exit 1
fi

# Upload to Cumulocity
echo "Uploading to Cumulocity..."
c8y applications createBinary -f --file pulsar-python-mapper-ms.zip --id pulsar-python-mapper-ms

# Check if upload was successful
if [ $? -eq 0 ]; then
    echo "✅ Deployment successful!"
    echo "Application uploaded to Cumulocity with ID: pulsar-python-mapper-ms"
else
    echo "❌ Deployment failed!"
    exit 1
fi