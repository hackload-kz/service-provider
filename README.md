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

### Partner
1. Start an order
   ```shell
   curl -X POST --verbose --location "http://localhost:8080/api/partners/v1/orders"
   ```
   The result
   ```json
   {
      "order_id": "98c123b9-fc80-4d6b-a3f6-0cbe84e0fd88"
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
          "id": "1bf2726c-b6b7-459f-be3f-8124ecd7c619",
          "row": 1,
          "seat": 1,
          "is_free": true
      }
   ]
   ```
3. Select a place for a started order
   ```shell
   curl -X PATCH --verbose --location "http://localhost:8080/api/partners/v1/places/1bf2726c-b6b7-459f-be3f-8124ecd7c619/select" \
    -H "Content-Type: application/json" \
    -d '{
          "order_id": "98c123b9-fc80-4d6b-a3f6-0cbe84e0fd88"
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
        "id": "1bf2726c-b6b7-459f-be3f-8124ecd7c619",
        "row": 1,
        "seat": 1,
        "is_free": false
      }
   ]
   ```
5. (Optionally) Release place. Place can be manually released only from started order
   ```shell
   curl -X PATCH --verbose --location "http://localhost:8080/api/partners/v1/places/1bf2726c-b6b7-459f-be3f-8124ecd7c619/release"
   ```
   The result does not have a response body
6. Get an order
   ```shell
   curl -X GET --verbose --location "http://localhost:8080/api/partners/v1/orders/98c123b9-fc80-4d6b-a3f6-0cbe84e0fd88"
   ```
   The result
   ```json
   {
      "id": "98c123b9-fc80-4d6b-a3f6-0cbe84e0fd88",
      "status": "STARTED",
      "started_at": 1753513916968,
      "updated_at": 1753513966871,
      "places_count": 1
   }
   ```
7. (Optionally) Cancel an order. A confirmed order can not be cancelled. It is a terminal status for an order. You can not do anything with the order anymore.
   ```shell
   curl -X PATCH --verbose --location "http://localhost:8080/api/partners/v1/orders/98c123b9-fc80-4d6b-a3f6-0cbe84e0fd88/cancel"
   ```
8. Submit an order. Now you can not add or remove places from the order. You can only confirm or cancel the order.
   ```shell
   curl -X PATCH --verbose --location "http://localhost:8080/api/partners/v1/orders/98c123b9-fc80-4d6b-a3f6-0cbe84e0fd88/submit"
   ```
9. Confirm an order. It is a terminal status for an order. You can not do anything with the order anymore.
   ```shell
   curl -X PATCH --verbose --location "http://localhost:8080/api/partners/v1/orders/98c123b9-fc80-4d6b-a3f6-0cbe84e0fd88/confirm"
   ```
