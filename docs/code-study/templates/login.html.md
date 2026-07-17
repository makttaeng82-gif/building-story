# login.html 코드 주석형 해설

원본 파일: `src/main/resources/templates/login.html`

형식:
- 원본 코드를 그대로 배치한다.
- 필요한 코드 바로 아래에 해설 주석을 붙인다.
- import/package/단순 선언은 과하게 설명하지 않는다.

```html
<!doctype html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>건물주이야기 로그인</title>
    <link rel="stylesheet" href="/styles.css">
</head>
<body>
<!--
로그인/회원가입 화면이다.
AuthController가 mode 값을 넘겨 같은 템플릿을 로그인과 가입 양쪽에 사용한다.
-->
<main class="auth-page">
    <section class="auth-hero">
        <span class="logo">B</span>
        <h1>건물주이야기</h1>
        <p>청주에서 시작하는 첫 월세.</p>
    </section>

    <section class="auth-grid">
        <form class="panel auth-card" method="post" action="/login">
        <!-- 해설: POST 폼이다. 버튼 클릭 시 서버 상태를 변경하는 요청을 보낸다. -->
            <h2>로그인</h2>
            <p th:if="${error}" class="error" th:text="${error}">오류</p>
            <!-- 해설: 서버 모델 값 조건에 따라 이 요소를 보여주거나 숨긴다. -->
            <label>
                닉네임
                <input name="username" autocomplete="username" required>
            </label>
            <label>
                비밀번호
                <input type="password" name="password" autocomplete="current-password" required>
            </label>
            <button class="wide" type="submit">로그인</button>
            <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
        </form>

        <form class="panel auth-card" method="post" action="/register">
        <!-- 해설: POST 폼이다. 버튼 클릭 시 서버 상태를 변경하는 요청을 보낸다. -->
            <h2>회원가입</h2>
            <label>
                닉네임
                <input name="username" autocomplete="username" required minlength="2" maxlength="20">
            </label>
            <label>
                비밀번호
                <input type="password" name="password" autocomplete="new-password" required minlength="4">
            </label>
            <button class="wide" type="submit">새로 시작</button>
            <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
        </form>
    </section>
</main>
</body>
</html>
```