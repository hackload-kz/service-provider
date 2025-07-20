# RTFM

## Local run

### Linux

#### Prerequisites:
1. Java 24. You can use https://sdkman.io/
2. Docker and docker-compose https://docs.docker.com/engine/install/ https://docs.docker.com/compose/install/

#### Steps:
1. Build maven modules
    ```shell
    sh build.sh
    ```
2. Run docker-compose
    ```shell
    sh run.sh
    ```

TO BE DONE...

## Usage

### Admin
1. Create a place. Be careful with this method, the system is unfinished, and it does not check duplicates in rows and seats
   ```shell
   curl -X POST --verbose --location "http://localhost:8080/api/admin/v1/places" \
    -H "Content-Type: application/json" \
    -d '{
          "row": 1,
          "seat": 1
        }'
   ```
   The result
   ```json
   {
      "place_id": "bdac691d-bb27-4a26-83d4-d70f5e56bac4"
   }
   ```

### Partner
1. Start an order
   ```shell
   curl -X POST --verbose --location "http://localhost:8080/api/partners/v1/orders"
   ```
   The result
   ```json
   {
      "order_id": "98ccd4a3-5a74-4e62-a3c1-77e14dad7b96"
   }
   ```
2. List places
   ```shell
   curl -X GET --verbose --location "http://localhost:8080/api/partners/v1/places?page=1&pageSize=20"
   ```
   The result
   ```json
   [
      {
          "id": "bdac691d-bb27-4a26-83d4-d70f5e56bac4",
          "row": 1,
          "seat": 1,
          "is_free": true
      }
   ]
   ```
3. Select a place for a started order
   ```shell
   curl -X PATCH --verbose --location "http://localhost:8080/api/partners/v1/places/bdac691d-bb27-4a26-83d4-d70f5e56bac4/select" \
    -H "Content-Type: application/json" \
    -d '{
          "order_id": "98ccd4a3-5a74-4e62-a3c1-77e14dad7b96"
        }'
   ```
   The result does not have a response body
4. List places and check that it is now is not free
   ```shell
   curl -X GET --verbose --location "http://localhost:8080/api/partners/v1/places?page=1&pageSize=20"
   ```
   The result
   ```json
   [
      {
        "id": "bdac691d-bb27-4a26-83d4-d70f5e56bac4",
        "row": 1,
        "seat": 1,
        "is_free": false
      }
   ]
   ```
5. (Optionally) Release place. Place can be manually released only from started order
   ```shell
   curl -X PATCH --verbose --location "http://localhost:8080/api/partners/v1/places/bdac691d-bb27-4a26-83d4-d70f5e56bac4/release"
   ```
   The result does not have a response body
6. Get an order
   ```shell
   curl -X GET --verbose --location "http://localhost:8080/api/partners/v1/orders/98ccd4a3-5a74-4e62-a3c1-77e14dad7b96"
   ```
   The result
   ```json
   {
      "id": "98ccd4a3-5a74-4e62-a3c1-77e14dad7b96",
      "status": "STARTED",
      "started_at": "2025-07-20T07:51:27Z",
      "updated_at": "2025-07-20T07:53:00Z",
      "places_count": 1
   }
   ```
7. (Optionally) Cancel an order. A confirmed order can not be cancelled. It is a terminal status for an order. You can not do anything with the order anymore.
   ```shell
   curl -X PATCH --verbose --location "http://localhost:8080/api/partners/v1/orders/98ccd4a3-5a74-4e62-a3c1-77e14dad7b96/cancel"
   ```
8. Submit an order. Now you can not add or remove places from the order. You can only confirm or cancel the order.
   ```shell
   curl -X PATCH --verbose --location "http://localhost:8080/api/partners/v1/orders/98ccd4a3-5a74-4e62-a3c1-77e14dad7b96/submit"
   ```
9. Confirm an order. It is a terminal status for an order. You can not do anything with the order anymore.
   ```shell
   curl -X PATCH --verbose --location "http://localhost:8080/api/partners/v1/orders/98ccd4a3-5a74-4e62-a3c1-77e14dad7b96/confirm"
   ```
