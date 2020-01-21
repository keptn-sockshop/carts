# Keptn-Sockshop/Carts

Within this repository we provide the sourcecode for the carts microservice which is onboarded to [Keptn](https://keptn.sh).

Please create all pull requests to the `master` branch.

## Faulty Item in cart

When adding an item to the cart, we simulate an algorithmic problem by exhausting the CPU with a loop.
This can be done by sending a `POST` request with a faulty item id (`03fef6ac-1896-4ce8-bd69-b798f85c6e0f`) to `/carts/1/items`, e.g.:

```
curl -d '{"itemId": "03fef6ac-1896-4ce8-bd69-b798f85c6e0f", "unitPrice": "99.99"}' -H "Content-Type: application/json" -X POST http://carts.sockshop-production.$(kubectl get cm keptn-domain -n keptn -o=jsonpath='{.data.app_domain}')/carts/1/items
```


## Different versions with slowdowns

We provide docker images with various application properties set:

| Image Name                    | Image Tag  | Properties (see [src/main/resources/application.properties](src/main/resources/application.properties)) |
|-------------------------------|------------|--------------------------------------------------|
| docker.io/keptnexamples/carts | 0.10.**1** | version=v1,promotionRate=0,delayInMillis=0       |
| docker.io/keptnexamples/carts | 0.10.**2** | version=v2,promotionRate=0,delayInMillis=1000ms  |
| docker.io/keptnexamples/carts | 0.10.**3** | version=v3,promotionRate=0,delayInMillis=0       |
