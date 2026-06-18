# CH 5 플러스 Spring 과제

Spring Boot 기반 기존 프로젝트에서 트랜잭션, JWT, JPQL, 테스트 코드, AOP, JPA Cascade, N+1, QueryDSL, Spring Security 관련 요구사항을 개선한 과제입니다.

---

## 프로젝트 정보

| 항목         | 내용                    |
| ---------- | --------------------- |
| Language   | Java 17               |
| Framework  | Spring Boot 3.3.3     |
| Build Tool | Gradle                |
| ORM        | Spring Data JPA       |
| Database   | H2, MySQL             |
| Auth       | JWT + Spring Security |
| Query      | JPQL, QueryDSL        |

---

## 실행 방법

```bash
./gradlew clean test
```

```bash
./gradlew bootRun
```

JWT 기능 사용을 위해 아래 설정이 필요합니다.

```properties
jwt.secret.key=<base64-encoded-secret-key>
```

테스트 환경에서는 `src/test/resources/application.properties`에 테스트용 secret key를 설정했습니다.

---

## 구현 내용

## Level 1

### 1. `@Transactional` 문제 해결

`TodoService` 클래스에 `@Transactional(readOnly = true)`가 적용되어 있어 Todo 저장 시 쓰기 쿼리가 실행되지 않는 문제가 있었습니다.

저장 기능인 `saveTodo()` 메서드에 별도로 `@Transactional`을 적용하여 쓰기 가능한 트랜잭션으로 실행되도록 수정했습니다.

---

### 2. JWT nickname 추가

User 정보에 `nickname`을 추가했습니다.

구현 내용은 다음과 같습니다.

* `User` 엔티티에 `nickname` 필드 추가
* `SignupRequest`에 `nickname` 필드 추가
* 회원가입 시 nickname 저장
* 회원가입/로그인 성공 시 JWT claim에 nickname 추가

nickname은 중복 가능하므로 unique 제약은 설정하지 않았습니다.

JWT payload에는 아래 정보가 포함됩니다.

```json
{
  "sub": "1",
  "email": "test@test.com",
  "nickname": "sungu",
  "userRole": "USER"
}
```

---

### 3. JPQL 검색 조건 추가

Todo 목록 조회 시 아래 조건으로 검색할 수 있도록 수정했습니다.

* `weather`
* 수정일 시작 조건 `startDate`
* 수정일 종료 조건 `endDate`

각 조건은 optional 값으로 처리했습니다.

요청 예시는 다음과 같습니다.

```http
GET /todos?page=1&size=10
```

```http
GET /todos?page=1&size=10&weather=Sunny
```

```http
GET /todos?page=1&size=10&startDate=2026-06-01T00:00:00&endDate=2026-06-18T23:59:59
```

Repository에서는 JPQL을 사용하여 조건이 없으면 해당 조건을 무시하고, 조건이 있으면 필터링되도록 구현했습니다.

---

### 4. Controller 테스트 수정

`todo_단건_조회_시_todo가_존재하지_않아_예외가_발생한다()` 테스트가 실패하던 문제를 수정했습니다.

테스트에서는 `InvalidRequestException`이 발생하도록 설정되어 있었지만, 기대 응답은 `200 OK`로 되어 있었습니다.

`GlobalExceptionHandler`에서 `InvalidRequestException`은 `400 BAD_REQUEST`로 처리되므로, 테스트 기대값을 `400 BAD_REQUEST`로 수정했습니다.

---

### 5. AOP 수정

관리자 권한 변경 로그 AOP가 잘못된 메서드와 시점에 적용되어 있었습니다.

기존에는 `UserController.getUser()` 실행 후 동작하도록 되어 있었지만, 요구사항에 맞게 `UserAdminController.changeUserRole()` 실행 전에 동작하도록 수정했습니다.

수정 내용은 다음과 같습니다.

* `@After` → `@Before`
* 대상 메서드 변경: `UserController.getUser()` → `UserAdminController.changeUserRole()`

---

## Level 2

### 6. JPA Cascade 적용

Todo를 생성할 때 생성한 유저가 담당자로 자동 등록되어야 하는 요구사항을 구현했습니다.

`Todo` 생성자에서는 `Manager` 객체를 생성하고 있었지만, Todo 저장 시 Manager가 함께 저장되지 않았습니다.

`Todo`와 `Manager`의 연관관계에 `CascadeType.PERSIST`를 추가하여 Todo 저장 시 Manager도 함께 저장되도록 수정했습니다.

---

### 7. N+1 문제 해결

댓글 목록 조회 시 댓글 작성자 User를 댓글 개수만큼 추가 조회하는 N+1 문제가 발생할 수 있었습니다.

`Comment.user`는 지연 로딩으로 설정되어 있었기 때문에, 댓글 조회 후 DTO 변환 과정에서 `comment.getUser()`를 호출하면 추가 쿼리가 발생할 수 있었습니다.

댓글 조회 JPQL에 fetch join을 적용하여 Comment 조회 시 User도 함께 조회되도록 수정했습니다.

---

### 8. QueryDSL 적용

`TodoService.getTodo()`에서 사용하던 JPQL 기반 `findByIdWithUser`를 QueryDSL로 변경했습니다.

구현 내용은 다음과 같습니다.

* QueryDSL 의존성 추가
* `QueryDslConfig` 추가
* `TodoRepositoryCustom` 추가
* `TodoRepositoryImpl`에서 QueryDSL로 `findByIdWithUser()` 구현
* Todo 단건 조회 시 User를 fetch join으로 함께 조회

이를 통해 Todo 단건 조회에서도 User 추가 조회가 발생하지 않도록 처리했습니다.

---

### 9. Spring Security 전환

기존 Filter와 ArgumentResolver 기반 인증 구조를 Spring Security 기반 구조로 변경했습니다.

기존 구조는 다음과 같았습니다.

```text
JwtFilter
→ request attribute에 userId, email, userRole 저장
→ @Auth AuthUser
→ AuthUserArgumentResolver
```

변경 후 구조는 다음과 같습니다.

```text
JwtFilter
→ SecurityContextHolder에 Authentication 저장
→ @AuthenticationPrincipal AuthUser
→ SecurityConfig에서 인증/인가 처리
```

구현 내용은 다음과 같습니다.

* Spring Security 의존성 추가
* `SecurityConfig` 추가
* `JwtFilter`를 `OncePerRequestFilter` 기반으로 변경
* JWT 검증 후 `SecurityContextHolder`에 인증 정보 저장
* 기존 `@Auth`를 `@AuthenticationPrincipal`로 변경
* 기존 `FilterConfig`, `WebConfig`, `AuthUserArgumentResolver`, `Auth` 제거
* `/admin/**` 권한 검사를 Spring Security의 `hasRole("ADMIN")`로 처리

---

## 테스트 결과

전체 테스트를 통과했습니다.

```bash
./gradlew clean test
```

```text
BUILD SUCCESSFUL
```

---

## 최종 체크리스트

* [x] `@Transactional` readOnly 문제 해결
* [x] User nickname 추가
* [x] JWT nickname claim 추가
* [x] JPQL 검색 조건 추가
* [x] Controller 테스트 수정
* [x] AOP 실행 대상 및 시점 수정
* [x] JPA Cascade 적용
* [x] 댓글 조회 N+1 문제 해결
* [x] Todo 단건 조회 QueryDSL 적용
* [x] Spring Security 기반 인증/인가 전환
* [x] 전체 테스트 통과
