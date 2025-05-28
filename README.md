# Sample Attachment Cordapp Usage Manual

## Overview

This manual explains how to upload/download an zip file to/from Corda node via Corda RPC Endpoint. 
The API allows you to send and download attachments between Corda nodes.

## 0. Prerequisites
- Make sure you have a running Corda node with the Sample Attachment Cordapp deployed. To run all the required nodes, you can use the following command in the `build/nodes` directory:
  ```bash
  ./runnodes
  ```
- Make sure the Corda RPC Server is running. To run the Corda RPC server, you can use the following command in `clients` directory:
  ```bash
  ../gradlew run templateServer
  ```
- Make sure you have put the ZIP file you want to send in an accessible location on the Corda node directory.

## 1. Send Attachment

### Endpoint

- **URL:** `/sendAttachment`
- **Method:** `POST`
- **Content-Type:** `application/json`
- **Response:** JSON object containing the transaction hash and Hash of the attachment uploaded, or an error message.

### Request Body

```json
{
  "receiver": "O=Buyer,L=London,C=GB",
  "zipPath": "/absolute/path/to/your/file.zip"
}
```

- `receiver`: The X500 name of the recipient party as a string. By default,  you can use `O=Seller,L=New York,C=US`
- `zipPath`: The absolute path to the ZIP file you want to send.

### Example cURL Command

```bash
curl -X POST http://localhost:10050/sendAttachment \
  -H "Content-Type: application/json" \
  -d '{
    "receiver": "O=Seller,L=New York,C=US",
    "zipPath": "/absolute/path/to/your/file.zip"
  }'
```

### Example Success Response

```json
{
  "txHash": "A1B2C3D4E5F6...",
  "attachmentId": "1234567890ABCDEF..."
}
```

### Example Error Response

```json
{
  "error": "java.lang.IllegalArgumentException: O=Buyer,L=London,C=GB not found in network map."
}
```

## 2. Download Attachment

### Endpoint

- **URL:** `/downloadAttachment`
- **Method:** `GET`
- **Content-Type:** `application/json`
- **Response:** JSON object with the result message or an error.

### Request Body

```json
{
  "attachmentId": "1234567890ABCDEF...",
  "path": "/absolute/path/to/save/file.zip"
}
```

- `attachmentId`: The ID of the attachment to download.
- `path`: The absolute path where the downloaded file should be saved.

### Example cURL Command

```bash
curl -X GET http://localhost:10050/downloadAttachment \
  -H "Content-Type: application/json" \
  -d '{
    "attachmentId": "1234567890ABCDEF...",
    "path": "/absolute/path/to/save/file.zip"
  }'
```

### Example Success Response

```json
{
  "result": "Downloaded file:1234567890ABCDEF... to /Users/bob/Downloads/received.zip"
}
```

### Example Error Response

```json
{
  "error": "org.springframework.web.server.ResponseStatusException: 404 NOT_FOUND \"Requested file: 1234567890ABCDEF... is not found.\""
}
```

---

## Notes

- Ensure the file paths provided are accessible and writable by the server process.
- The X500 names and attachment IDs must match those registered in your Corda.
- All requests and responses use JSON format.
