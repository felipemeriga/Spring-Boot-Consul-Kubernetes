# Spring Boot Consul Kubernetes
In this project I'm demonstrating you how to use Hashicorp's **Consul** as a discovery and configuration server with [Spring Cloud Consul](https://spring.io/projects/spring-cloud-consul) and other Spring Cloud projects for building microservice-based architecture.

## Getting Started
1. To build and run sample applications you need to have Maven, JDK11+ and Docker. However, the simplest way to start with it is through any IDE like Intellij or Eclipse.
2. First, to run it locally you have to run Consul on Docker container, bellow we also have a section for deploying it on Kubernetes.
```
$ docker run -d --name consul-1 -p 8500:8500 -e CONSUL_BIND_INTERFACE=eth0 consul
```
3. Then you can compile your application with Maven `mvn clean install` command and using `java -jar ...` command. Or you can just build it and run using your IDE. Each application is listeting on dynamically generated port.

## Architecture
Our sample microservices-based system consists of the following modules:
- **gateway-service** - a module that uses Spring Cloud Gateway for running Spring Boot application that acts as a proxy/gateway in our architecture.
- **account-service** -  a module containing the first of our sample microservices that allows to perform CRUD operation on in-memory repository of accounts
- **customer-service** - a module containing the second of our sample microservices that allows to perform CRUD operation on in-memory repository of customers. It communicates with account-service.
- **product-service** - a module containing the third of our sample microservices that allows to perform CRUD operation on in-memory repository of products.
- **order-service** - a module containing the fourth of our sample microservices that allows to perform CRUD operation on in-memory repository of orders. It communicates with all other microservices.

The following picture illustrates the architecture described above.

<img src="https://piotrminkowski.files.wordpress.com/2019/11/microservices-consul-1-1.png" title="Architecture"><br/>

When running sample applications we can test more advanced scenario. We may leverage **Zone Affinity** mechanism to prefer communication inside a single zone. We can also start a cluster of Consul modes started locally on Docker containers. Here's the picture illustrating such an architecture:

<img src="https://piotrminkowski.files.wordpress.com/2019/11/microservices-consul-2.png" title="Architecture"><br/>

## Description
Detailed description can be found here: [Microservices with Spring Boot, Spring Cloud Gateway and Consul Cluster](https://piotrminkowski.wordpress.com/2019/11/06/microservices-with-spring-boot-spring-cloud-gateway-and-consul-cluster/)

## Deploying on Kubernetes

As Kubernetes is also a service discovery tool as Consul, and as we are going to use only consul to this activity, we have
to make sure that the pods running Consul must have a really reliable and stable network, because all the services will depend on the
containers deployed on the pods. So, using deployments or pods may leave to connection issues between the services, because
the orchestrator will not guarantee network stability. There is two ways of ensuring that, the first one is using Helm
charts, which is a tool that helps us provisioning services in Kubernetes in a easier way that the standard Kubernetes yaml files. The second
is using the Kubernetes StatefulSet, which guarantee network stability. For the both options, we the conceptes are defined here:
- [Consul with Helm](https://www.consul.io/docs/platform/k8s/run.html)
- [StatefulSet Kubernetes](https://kubernetes.io/docs/concepts/workloads/controllers/statefulset/)

The advantage about using Helm is that you have an abstraction from complex things, but when talking specifically about Spring Boot Microservices, 
the Consul containers provided from Helm in many times will result on service discovery issue, mainly because of the poor documentation about the usage of 
Helm, Consul and Spring Boot together. So, that is why we decided to go to the StatefulSet approach, because was the one who showed more stability along a set of practical tests.

Together with Consul, we will use Vault, which is a service for securely accessing secrets, without exposing that directly in the Kubernetes cluster.

### Deploying Consul and Vault on Kubernetes

In the following tutorial we'll walk you through provisioning a highly-available Hashicorp Vault and Consul cluster on Kubernetes with TLS.

Main dependencies:

- Vault v1.2.4
- Consul v1.6.1
- Kubernetes v1.16.2

All the files related to Kubernetes configuration, TLS and Vault are located in the kubernetes-vault directory.

#### TLS Certificates

TLS will be used to secure RPC communication between each Consul member. To set this up, we'll create a Certificate Authority (CA) to sign the certificates, via CloudFlare's [SSL ToolKit](https://github.com/cloudflare/cfssl) (cfssl and cfssljson), and distribute keys to the nodes.
Start by installing Go if you don't already have it.
```
$ brew update
$ brew install go
```

Once installed, create a workspace, configure the GOPATH and add the workspace's bin folder to your system path:

```
$ mkdir $HOME/go
$ export GOPATH=$HOME/go
$ export PATH=$PATH:$GOPATH/bin
```

Next, install the SSL ToolKit:

```
$ go get -u github.com/cloudflare/cfssl/cmd/cfssl
$ go get -u github.com/cloudflare/cfssl/cmd/cfssljson
```

Create a new project directory called "vault-consul-kubernetes" and add the following files and folders:
```
├── certs
│   ├── config
│   │   ├── ca-config.json
│   │   ├── ca-csr.json
│   │   ├── consul-csr.json
│   │   └── vault-csr.json
├── consul
└── vault
```

ca-config.json:
```
{
  "signing": {
    "default": {
      "expiry": "87600h"
    },
    "profiles": {
      "default": {
        "usages": [
          "signing",
          "key encipherment",
          "server auth",
          "client auth"
        ],
        "expiry": "8760h"
      }
    }
  }
}
```

ca-csr.json:
```
{
  "hosts": [
    "cluster.local"
  ],
  "key": {
    "algo": "rsa",
    "size": 2048
  },
  "names": [
    {
      "C": "US",
      "ST": "Colorado",
      "L": "Denver"
    }
  ]
}
```

consul-csr.json:
```
{
  "CN": "server.dc1.cluster.local",
  "hosts": [
    "server.dc1.cluster.local",
    "127.0.0.1"
  ],
  "key": {
    "algo": "rsa",
    "size": 2048
  },
  "names": [
    {
      "C": "US",
      "ST": "Colorado",
      "L": "Denver"
    }
  ]
}
```

vault-csr.json:
```
{
  "hosts": [
    "vault",
    "127.0.0.1"
  ],
  "key": {
    "algo": "rsa",
    "size": 2048
  },
  "names": [
    {
      "C": "US",
      "ST": "Colorado",
      "L": "Denver"
    }
  ]
}
```

Create a Certificate Authority:
```
$ cfssl gencert -initca certs/config/ca-csr.json | cfssljson -bare certs/ca
```

Then, create a private key and a TLS certificate for Consul:
```
$ cfssl gencert \
    -ca=certs/ca.pem \
    -ca-key=certs/ca-key.pem \
    -config=certs/config/ca-config.json \
    -profile=default \
    certs/config/consul-csr.json | cfssljson -bare certs/consul
```

Do the same for Vault:
```
$ cfssl gencert \
    -ca=certs/ca.pem \
    -ca-key=certs/ca-key.pem \
    -config=certs/config/ca-config.json \
    -profile=default \
    certs/config/vault-csr.json | cfssljson -bare certs/vault
```
You should now see the following PEM files within the "certs" directory:

- ca-key.pem
- ca.pem
- consul-key.pem
- consul.pem
- vault-key.pem
- vault.p

#### Consul

#### Gossip Encryption Key

Consul uses the Gossip protocol to broadcast encrypted messages and discover new members added to the cluster. This requires a shared key. To generate, first install the Consul client (Mac users should use Brew for this -- ```brew upgrade consul```), and then generate a key and store it in an environment variable:
```
$ export GOSSIP_ENCRYPTION_KEY=$(consul keygen)
```

Store the key along with the TLS certificates in a Secret:
```
$ kubectl create secret generic consul \
  --from-literal="gossip-encryption-key=${GOSSIP_ENCRYPTION_KEY}" \
  --from-file=certs/ca.pem \
  --from-file=certs/consul.pem \
  --from-file=certs/consul-key.pem
```

Verify:
```
$ kubectl describe secrets consul
```

You should see:
```
Name:         consul
Namespace:    default
Labels:       <none>
Annotations:  <none>

Type:  Opaque

Data
====
ca.pem:                 1168 bytes
consul-key.pem:         1679 bytes
consul.pem:             1359 bytes
gossip-encryption-key:  44 bytes
```
#### Config

Add a new file to "consul" called config.json:
```
  "ca_file": "/etc/tls/ca.pem",
  "cert_file": "/etc/tls/consul.pem",
  "key_file": "/etc/tls/consul-key.pem",
  "verify_incoming": true,
  "verify_outgoing": true,
  "verify_server_hostname": true,
  "ports": {
    "https": 8443
  }
}
```

By setting ```verify_incoming```, ```verify_outgoing``` and ```verify_server_hostname``` to ```true``` all RPC calls must be encrypted.

Save this config in a ConfigMap:
```
$ kubectl create configmap consul --from-file=consul/config.json
$ kubectl describe configmap consul
```

#### Creating the Kubernetes Service, and StatefulSet

In order to create the service, just execute the following command:
```
$ kubectl create -f kubernetes-vault/consul/service.yaml
$ kubectl get service consul
```

The StatefulSet will follow the same logic, and after creating you will see
the pods with the Consul containers running.
```
kubectl create -f kubernetes-vault/consul/statefulset.yaml
```

In order to check the Consul dashboard, do the following command:
```
kubectl port-forward consul-1 8500:8500
```

## Spring Boot Profiles

As this samples of microservices applications are ready to be executed locally, or 
deployed on Kubernetes, actually in ```application.yml``` and ```bootstrap.yml``` files 
there are four different profiles:

- prod-zone1
- prod-zone2
- dev-zone1
- dev-zone2

The prod ones are for being deployed in Kubernetes, and they are pointing to http://consul-0.consul.default.svc.cluster.local
which is the DNS for the first Consul server defined by the StatefulSet. For the dev it's simply pointing to
localhost on port 8500.

For the suffixes zone1 and zone2, are related to the Consul zones that
were explained before.

## Vault

Moving right along, let's configure Vault to run on Kubernetes.

### Secret

Store the Vault TLS certificates that we created in a Secret:
```
$ kubectl create secret generic vault \
    --from-file=certs/ca.pem \
    --from-file=certs/vault.pem \
    --from-file=certs/vault-key.pem

$ kubectl describe secrets vault
```

### ConfigMap

Add a new file for the Vault config called vault/config.json:
```
{
  "listener": {
    "tcp":{
      "address": "127.0.0.1:8200",
      "tls_disable": 0,
      "tls_cert_file": "/etc/tls/vault.pem",
      "tls_key_file": "/etc/tls/vault-key.pem"
    }
  },
  "storage": {
    "consul": {
      "address": "consul:8500",
      "path": "vault/",
      "disable_registration": "true",
      "ha_enabled": "true"
    }
  },
  "ui": true
}
```

Here, we configured Vault to use the Consul backend (which supports high availability), defined the TCP listener for Vault, enabled TLS, added the paths to the TLS certificate and the private key, and enabled the Vault UI. Review the docs for more info on configuring Vault.

Save this config in a ConfigMap:
```
$ kubectl create configmap vault --from-file=vault/config.json
$ kubectl describe configmap vault
```

### Vault to Kubernetes

Now that all the config maps are created and all the configuration, let's deploy Vault on
Kubernetes, just go to the kubernetes-vault/vault/ directory, and create the service and deployment.
```
$ kubectl create -f vault/service.yaml
$ kubectl get service vault

$ kubectl apply -f vault/deployment.yaml
```





