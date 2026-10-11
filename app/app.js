(function () {
  const L = window.LESSON;
  const stage = document.getElementById("stage");
  const tabs = document.querySelectorAll(".tabbar button");
  const state = {
    tab: "today",
    step: "teach",
    recall: "",
    quizIndex: 0,
    picked: null,
    mastered: false
  };

  tabs.forEach((btn) => {
    btn.addEventListener("click", () => {
      state.tab = btn.dataset.tab;
      tabs.forEach((b) => b.classList.toggle("is-on", b === btn));
      render();
    });
  });

  function render() {
    const html = {
      today: today,
      shelf: shelf,
      review: review,
      me: me
    }[state.tab]();
    stage.innerHTML = `<section class="screen">${html}</section>`;
    bind();
  }

  function today() {
    if (state.step === "teach") return teach();
    if (state.step === "recall") return recall();
    return quiz();
  }

  function teach() {
    const p = L.teach.paragraphs.map((t) => `<p>${t}</p>`).join("");
    return `
      <p class="kicker">${L.book.chapter}</p>
      <h1>${L.teach.title}</h1>
      ${steps(0)}
      <article class="card prose">${p}</article>
      <button class="primary" data-go="recall">我读完了，用自己的话说</button>
    `;
  }

  function recall() {
    return `
      <p class="kicker">复述</p>
      <h1>不要抄刚才的句子</h1>
      ${steps(1)}
      <article class="card">
        <p class="meta">${L.teach.ask}</p>
        <div class="prompt">
          <textarea id="recall" placeholder="用你自己的话写两句">${escapeHtml(state.recall)}</textarea>
        </div>
      </article>
      <button class="primary" id="to-quiz">去检验</button>
    `;
  }

  function quiz() {
    const item = L.quiz[state.quizIndex];
    const choices = item.options.map((opt, i) => {
      let cls = "choice";
      if (state.picked !== null) {
        if (i === item.answer) cls += " is-right";
        else if (i === state.picked) cls += " is-wrong";
      }
      return `<button class="${cls}" data-pick="${i}">${opt}</button>`;
    }).join("");
    const fb = state.picked === null ? "" : `
      <p class="feedback"><b>${state.picked === item.answer ? "对了。" : "还差一点。"}</b> ${item.why}</p>
    `;
    const next = state.picked === null ? "" : (
      state.quizIndex < L.quiz.length - 1
        ? `<button class="primary" id="next-q">下一题</button>`
        : `<button class="primary" id="done">今天这段先到这里</button>`
    );
    return `
      <p class="kicker">第 ${state.quizIndex + 1} / ${L.quiz.length} 题</p>
      <h1>${item.q}</h1>
      ${steps(2)}
      ${choices}
      ${fb}
      ${next}
    `;
  }

  function shelf() {
    const pct = Math.round(L.book.progress * 100);
    return `
      <p class="kicker">书架</p>
      <h1>在学的书</h1>
      <p class="lede">一次只推进一本。上传新书是下一版的事，这一版先把学习路径走通。</p>
      <article class="card book">
        <div class="spine">阅</div>
        <div>
          <h2>${L.book.title}</h2>
          <p class="meta">${L.book.author} · 已学 ${pct}%</p>
          <div class="progress" aria-hidden="true"><span style="width:${pct}%"></span></div>
        </div>
      </article>
      <button class="primary" data-tabjump="today">继续第三章</button>
    `;
  }

  function review() {
    const items = L.reviews.map((r) => `
      <article class="card">
        <div class="row">
          <h2>${r.title}</h2>
          <span class="due">${r.due}</span>
        </div>
        <p class="meta">${r.book}</p>
      </article>
    `).join("");
    return `
      <p class="kicker">复习</p>
      <h1>到期的，才出现在这里</h1>
      <p class="lede">没到期的不列出来。复习是为了在快忘的时候再遇见一次。</p>
      <div class="list">${items}</div>
      <button class="primary" data-tabjump="today">先复习今天到期的</button>
    `;
  }

  function me() {
    return `
      <p class="kicker">我的</p>
      <h1>学习记录</h1>
      <article class="card">
        <h2>${state.mastered ? "今天这段已检验" : "今天还没检验"}</h2>
        <p class="meta">版本 ${L.version} · 数据只在这台设备的这一页里，没有账号，也没有上传。</p>
      </article>
      <article class="card">
        <h2>接下来</h2>
        <p class="meta">0.2 才会接上真实的书籍上传和模型讲解。这一版故意不假装已经接上。</p>
      </article>
    `;
  }

  function steps(index) {
    const names = ["听懂", "复述", "做题"];
    return `<div class="steps">${names.map((n, i) => `<span class="${i === index ? "is-on" : ""}">${n}</span>`).join("")}</div>`;
  }

  function bind() {
    stage.querySelectorAll("[data-go]").forEach((btn) => {
      btn.addEventListener("click", () => {
        state.step = btn.dataset.go;
        render();
      });
    });
    const toQuiz = document.getElementById("to-quiz");
    if (toQuiz) {
      toQuiz.addEventListener("click", () => {
        state.recall = document.getElementById("recall").value.trim();
        if (state.recall.length < 8) {
          toQuiz.textContent = "再写具体一点";
          return;
        }
        state.step = "quiz";
        state.picked = null;
        render();
      });
    }
    stage.querySelectorAll("[data-pick]").forEach((btn) => {
      btn.addEventListener("click", () => {
        if (state.picked !== null) return;
        state.picked = Number(btn.dataset.pick);
        render();
      });
    });
    const next = document.getElementById("next-q");
    if (next) {
      next.addEventListener("click", () => {
        state.quizIndex += 1;
        state.picked = null;
        render();
      });
    }
    const done = document.getElementById("done");
    if (done) {
      done.addEventListener("click", () => {
        state.mastered = true;
        state.tab = "me";
        tabs.forEach((b) => b.classList.toggle("is-on", b.dataset.tab === "me"));
        render();
      });
    }
    stage.querySelectorAll("[data-tabjump]").forEach((btn) => {
      btn.addEventListener("click", () => {
        state.tab = btn.dataset.tabjump;
        if (state.tab === "today") state.step = "teach";
        tabs.forEach((b) => b.classList.toggle("is-on", b.dataset.tab === state.tab));
        render();
      });
    });
  }

  function escapeHtml(s) {
    return s.replace(/[&<>"']/g, (c) => ({
      "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
    }[c]));
  }

  render();
})();
