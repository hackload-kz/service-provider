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
    docker-compose --file docker-compose/docker-compose.yaml up --detach
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
      "place_id": "ed8897a4-26e6-4fcc-a645-811f26cb673c"
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
      "order_id": "4d9ea8d5-d0d8-4e87-bea4-275a02f9d6ef"
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
          "id": "ed8897a4-26e6-4fcc-a645-811f26cb673c",
          "row": 1,
          "seat": 1,
          "is_free": true
      }
   ]
   ```
3. Select a place for a started order
   ```shell
   curl -X PATCH --verbose --location "http://localhost:8080/api/partners/v1/places/bc8dcae3-4174-4a93-8562-9d88f59f2711/select" \
    -H "Content-Type: application/json" \
    -d '{
          "order_id": "915e008e-3b84-4da4-a7a7-5835bf11a5b1"
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
        "id": "ed8897a4-26e6-4fcc-a645-811f26cb673c",
        "row": 1,
        "seat": 1,
        "is_free": false
      }
   ]
   ```
5. (Optionally) Release place. Place can be manually released only from started order
   ```shell
   curl -X PATCH --verbose --location "http://localhost:8080/api/partners/v1/places/3d058a47-f7da-411a-85ba-3ccb9cd6e2d3/release"
   ```
   The result does not have a response body
6. Get an order
   ```shell
   curl -X GET --verbose --location "http://localhost:8080/api/partners/v1/orders/915e008e-3b84-4da4-a7a7-5835bf11a5b1"
   ```
   The result
   ```json
   {
      "id": "4d9ea8d5-d0d8-4e87-bea4-275a02f9d6ef",
      "status": "STARTED",
      "started_at": "2025-07-18T07:04:39Z",
      "updated_at": "2025-07-18T07:06:38Z",
      "places_count": 1
   }
   ```
7. (Optionally) Cancel an order. A confirmed order can not be cancelled
   ```shell
   curl -X PATCH --verbose --location "http://localhost:8080/api/partners/v1/orders/915e008e-3b84-4da4-a7a7-5835bf11a5b1/cancel"
   ```
8. Submit an order. Now you can not add or remove places from the order. You can only confirm or cancel the order.
   ```shell
   curl -X PATCH --location "http://localhost:8080/api/partners/v1/orders/6aae518b-0336-4693-b56f-16f83912dff1/submit"
   ```
9. Confirm an order. It is a terminal status for an order. You can not do anything with the order anymore.
   ```shell
   curl -X PATCH --location "http://localhost:8080/api/partners/v1/orders/6aae518b-0336-4693-b56f-16f83912dff1/confirm"
   ```
