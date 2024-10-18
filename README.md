# https://animal-park.tistory.com/441
===============================================================================================================
5. 제공한 Sock shop 애플리케이션을 k8s 에 deploy 하고, 문제점 및 해결 방법을 작성하세요.
5.1 Sock shop 서비스 웹에서 접근 (화면 스크린샷)
## 5.2 Sock shop 애플리케이션의 각 마이크로 서비스간의 관계와 구조를 파악 결과를 제출

Sock Shop 애플리케이션은 여러 마이크로서비스로 구성되어 있으며, 각 서비스는 고유의 배포 및 서비스 YAML 구성 파일을 가지고 있습니다. 다음은 주요 구성 요소 및 역할에 대한 개요입니다:

### 마이크로서비스 구조 및 관계:

**프론트 엔드:**

- **front-end-dep.yaml:** 사용자 인터페이스인 프론트 엔드의 배포 구성.
- **front-end-svc.yaml:** 프론트 엔드를 노출하기 위한 서비스 구성.

**카탈로그 서비스:**

- **catalogue-dep.yaml:** 제품 카탈로그 데이터를 관리.
- **catalogue-svc.yaml:** 카탈로그를 노출하는 서비스.
- **catalogue-db-dep.yaml 및 catalogue-db-svc.yaml:** 제품 정보를 저장하기 위한 데이터베이스 서비스 및 배포.

**장바구니 서비스:**

- **carts-dep.yaml:** 사용자 장바구니를 관리.
- **carts-db-dep.yaml 및 carts-db-svc.yaml:** 장바구니 데이터베이스 저장소.

**주문 서비스:**

- **orders-dep.yaml:** 사용자 주문을 처리.
- **orders-db-dep.yaml 및 orders-db-svc.yaml:** 주문 저장을 위한 데이터베이스 서비스.

**사용자 서비스:**

- **user-dep.yaml:** 사용자 관련 기능을 관리.
- **user-svc.yaml:** 사용자 서비스를 노출.
- **user-db-dep.yaml 및 user-db-svc.yaml:** 사용자 데이터를 위한 데이터베이스.

**배송 서비스:**

- **shipping-dep.yaml:** 배송 로직을 처리.
- **shipping-svc.yaml:** 배송을 위한 서비스.

**결제 서비스:**

- **payment-dep.yaml:** 결제를 관리.
- **payment-svc.yaml:** 결제 처리를 위한 서비스.

**메시지 큐 (RabbitMQ):**

- **rabbitmq-dep.yaml 및 rabbitmq-svc.yaml:** 서비스 간 메시지 큐를 위해 RabbitMQ 사용.

**큐 마스터:**

- **queue-master-dep.yaml 및 queue-master-svc.yaml:** 메시지 큐 및 작업 분배 관리.

**세션 데이터베이스:**

- **session-db-dep.yaml 및 session-db-svc.yaml:** 세션 데이터 관리.

### 아키텍처에서 확인된 문제점:

**강한 결합:** 서비스 간의 의존성이 여전히 과도할 수 있으며, 하나의 서비스 또는 메시지 큐의 실패가 여러 서비스에 영향을 미칠 수 있습니다.

**확장성 제한:** RabbitMQ 또는 데이터베이스 서비스와 같은 일부 서비스는 부하가 증가할 경우 병목 현상이 발생할 수 있습니다.

**내결함성 부족:** 데이터베이스 및 큐에 대한 정의된 내결함성 메커니즘(예: 장애 조치 또는 복제)이 없는 것으로 보입니다.

## 5.3 현재 Sock shop 마이크로 서비스의 문제점 및 해결 방안을 제시하고, 수정한 yaml 파일을 작성합니다.

### 제안된 솔루션:

**복원력 향상:**

- 서비스에서 회로 차단기 및 재시도 메커니즘을 도입하여 실패를 보다 우아하게 처리.
- RabbitMQ 및 데이터베이스와 같은 중요 서비스에 대한 상태 점검 및 자동 확장을 구현.

**데이터베이스 최적화:**

- 사용자, 주문 및 카탈로그 데이터베이스가 내결함성과 확장성을 갖추도록 데이터베이스 복제 및 샤딩 구현.

**서비스 메시:**

- Istio와 같은 서비스 메시를 도입하면 서비스 간 통신을 보다 잘 제어할 수 있어 가시성, 로드 밸런싱 및 재시도를 가능하게 합니다.

**탈결합:**

- Kafka와 같은 기술을 사용하여 이벤트 기반 아키텍처를 도입하여 서비스 간의 동기 호출에 대한 의존성을 줄이는 것을 고려하십시오. 이렇게 하면 시스템이 개별 서비스 실패에 더 강해집니다.

이러한 제안은 Sock Shop 마이크로서비스 아키텍처의 전반적인 성능, 확장성 및 복원력을 향상시킬 것입니다.

아래는 제안된 해결 방안을 위한 일부 YAML 파일 예시입니다. 이 YAML 파일들은 주요 개선 사항인 **Circuit Breaker**, **Auto-scaling**, **Database Replication** 및 **Service Mesh**를 반영합니다.

### 1. **Circuit Breaker** (Istio 적용)
Circuit Breaker는 Istio와 같은 **Service Mesh**를 통해 구현할 수 있습니다. Istio VirtualService에서 설정할 수 있는 Circuit Breaker 예시입니다.

#### `catalogue-virtual-service.yaml` (Istio VirtualService)
```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: catalogue
spec:
  hosts:
  - catalogue
  http:
  - route:
    - destination:
        host: catalogue
    retries:
      attempts: 3
      perTryTimeout: 2s
      retryOn: 5xx
    fault:
      delay:
        percentage:
          value: 5
        fixedDelay: 3s
      abort:
        percentage:
          value: 10
        httpStatus: 500
```

이 설정은 `catalogue` 서비스에서 **3회 재시도**하고, 응답 지연이 발생할 경우 **3초 딜레이**를 주는 구조입니다. 또한, 일정 비율의 오류 응답을 시뮬레이션할 수 있습니다.

---

### 2. **Auto-scaling** (Horizontal Pod Autoscaler)
서비스를 자동으로 확장하기 위한 Kubernetes Horizontal Pod Autoscaler (HPA) 설정 파일입니다.

#### `catalogue-hpa.yaml`
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: catalogue
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: catalogue
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 50
```

이 HPA는 `catalogue` 서비스가 CPU 사용률이 50%를 초과할 경우 자동으로 인스턴스를 추가하여 최대 10개까지 확장하도록 설정합니다.

---

### 3. **Database Replication**
데이터베이스의 고가용성을 위한 **replication** 설정을 추가할 수 있습니다. 예를 들어, MongoDB를 사용하는 경우 복제 세트를 설정할 수 있습니다.

#### `user-db-statefulset.yaml` (MongoDB Replication)
```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: user-db
spec:
  serviceName: "user-db"
  replicas: 3
  selector:
    matchLabels:
      app: user-db
  template:
    metadata:
      labels:
        app: user-db
    spec:
      containers:
      - name: mongodb
        image: mongo:4.4
        ports:
        - containerPort: 27017
        command:
        - mongod
        - "--replSet"
        - rs0
  volumeClaimTemplates:
  - metadata:
      name: mongo-data
    spec:
      accessModes: ["ReadWriteOnce"]
      resources:
        requests:
          storage: 1Gi
```

이 설정은 `user-db`를 MongoDB 복제 세트로 실행하는 `StatefulSet`입니다. 이를 통해 **고가용성**과 **데이터 복제**를 보장할 수 있습니다.

---

### 4. **Event-Driven Architecture** (Kafka 배포 예시)
RabbitMQ 대신 **Kafka**와 같은 이벤트 스트리밍 플랫폼을 사용하여 서비스 간의 의존성을 낮출 수 있습니다.

#### `kafka-deployment.yaml`
```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: kafka
spec:
  serviceName: "kafka"
  replicas: 3
  selector:
    matchLabels:
      app: kafka
  template:
    metadata:
      labels:
        app: kafka
    spec:
      containers:
      - name: kafka
        image: wurstmeister/kafka:2.13-2.6.0
        ports:
        - containerPort: 9092
        env:
        - name: KAFKA_BROKER_ID
          valueFrom:
            fieldRef:
              fieldPath: metadata.name
        - name: KAFKA_ZOOKEEPER_CONNECT
          value: zookeeper:2181
        - name: KAFKA_ADVERTISED_LISTENERS
          value: PLAINTEXT://kafka:9092
  volumeClaimTemplates:
  - metadata:
      name: kafka-data
    spec:
      accessModes: ["ReadWriteOnce"]
      resources:
        requests:
          storage: 5Gi
```

이 배포 파일은 Kafka 클러스터의 노드를 3개 복제하는 StatefulSet으로 설정되어 있으며, 각 노드는 Zookeeper와 통신하여 메시징을 관리합니다.

---

이러한 YAML 파일들은 Sock Shop 애플리케이션의 문제를 해결하기 위해 제안된 주요 아키텍처 변경 사항들을 반영한 것입니다.



===============================================================================================================
 
