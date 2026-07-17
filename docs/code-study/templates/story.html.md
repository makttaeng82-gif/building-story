# story.html 코드 주석형 해설

원본 파일: `src/main/resources/templates/story.html`

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
    <title>건물주이야기 프롤로그</title>
    <link rel="stylesheet" href="/styles.css">
</head>
<body>
<!--
처음 시작 스토리 화면이다.
완료 POST가 성공하면 GameService.completeStory가 초기 자금, 첫 건물, 첫 매물을 만든다.
-->
<main class="story-page">
    <header class="story-head">
        <span class="logo">B</span>
        <div>
            <h1>건물주이야기: 첫 번째 월세</h1>
            <p>첫 로그인 프롤로그</p>
        </div>
    </header>

    <section class="story-grid">
        <article class="story-card">
            <img class="story-image" src="/assets/story/story-1.jpg" alt="퇴근길에 지친 주인공">
        </article>
        <article class="story-card">
            <img class="story-image" src="/assets/story/story-2.jpg" alt="신비한 존재의 위로">
        </article>
        <article class="story-card">
            <img class="story-image" src="/assets/story/story-3.jpg" alt="건물 관리 노하우 전수">
        </article>
        <article class="story-card">
            <img class="story-image" src="/assets/story/story-4.jpg" alt="원룸 건물을 받은 주인공">
        </article>
    </section>

    <form method="post" action="/story/complete" class="story-action">
    <!-- 해설: POST 폼이다. 버튼 클릭 시 서버 상태를 변경하는 요청을 보낸다. -->
        <button type="submit">청주 원룸 받고 시작하기</button>
        <!-- 해설: 사용자가 클릭해 폼 제출이나 프론트 동작을 실행하는 버튼이다. -->
    </form>
</main>
</body>
</html>
```