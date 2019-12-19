# Keptn-Sockshop/Carts

Within this repository we provide the sourcecode for the carts microservice which is onboarded to [Keptn](https://keptn.sh).

Please create all pull requests to the `master` branch.

## Different versions with slowdowns

We provide docker images with various application properties set:

| Image Name                    | Image Tag  | Properties (see [src/main/resources/application.properties](src/main/resources/application.properties)) |
|-------------------------------|------------|--------------------------------------------------|
| docker.io/keptnexamples/carts | 0.10.**1** | version=v1,promotionRate=0,delayInMillis=0       |
| docker.io/keptnexamples/carts | 0.10.**2** | version=v2,promotionRate=0,delayInMillis=1000ms  |
| docker.io/keptnexamples/carts | 0.10.**3** | version=v3,promotionRate=0,delayInMillis=0       |
