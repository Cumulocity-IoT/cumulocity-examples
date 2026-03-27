FROM  python:3.14-alpine

# Set working directory
WORKDIR /app


# Copy requirements and install Python dependencies
COPY requirements.txt .
RUN pip install  -r requirements.txt

# Copy application code
COPY pulsar-python-mapper-ms.py .


# Run the application
CMD ["python", "-u", "pulsar-python-mapper-ms.py"]
