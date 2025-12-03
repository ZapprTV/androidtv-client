package stream.zappr.tv

internal val ANDROID_TV_KEY_FILTER_JS = """
    (function() {
      if (window.__androidTvKeyFilterInstalled) {
        console.log('androidtv: key filter already installed');
        return;
      }
      window.__androidTvKeyFilterInstalled = true;
      
      console.log('androidtv: installing key filter');

      window.__androidTvIsPlaying = false;
      window.__androidTvMenuMode = false;
      window.__androidTvGlobalUnblock = false;

      var __androidTvVideo = null;

      function findVideoElement() {
        if (__androidTvVideo && document.contains(__androidTvVideo)) {
          return __androidTvVideo;
        }
        var v = document.querySelector('video');
        if (!v) {
          console.log('androidtv: no <video> element found yet');
          return null;
        }
        __androidTvVideo = v;
        console.log('androidtv: video element found, src=', v.currentSrc || v.src || '(none)');
        
        function updatePlayingFlag() {
          var playing = !v.paused && !v.ended;
          window.__androidTvIsPlaying = playing;
          console.log('androidtv: video state changed, isPlaying=', playing);
        }

        v.addEventListener('play', updatePlayingFlag);
        v.addEventListener('pause', updatePlayingFlag);
        v.addEventListener('ended', updatePlayingFlag);
        v.addEventListener('loadeddata', updatePlayingFlag);
        v.addEventListener('loadedmetadata', updatePlayingFlag);

        updatePlayingFlag();
        return v;
      }

      // primo tentativo
      findVideoElement();

      // se ricreano il player al cambio canale, lo ritroviamo
      var mo = new MutationObserver(function() {
        findVideoElement();
      });
      try {
        mo.observe(document.documentElement || document.body, {
          childList: true,
          subtree: true
        });
      } catch (err) {
        console.log('androidtv: MutationObserver error', err && err.message);
      }

      document.addEventListener('keydown', function(e) {
        var key = e.key || '';
        var code = e.keyCode || e.which;

        var target = e.target || {};
        var active = document.activeElement || {};

        var isUp    = key === 'ArrowUp'    || key === 'Up'    || code === 38;
        var isDown  = key === 'ArrowDown'  || key === 'Down'  || code === 40;
        var isEnter = key === 'Enter'      || code === 13;
        var isBack  = key === 'Backspace'  || key === 'Escape' || code === 8 || code === 27;

        var isLeft  = key === 'ArrowLeft'  || key === 'Left'  || code === 37;
        var isRight = key === 'ArrowRight' || key === 'Right' || code === 39;

        try {
          console.log(
            'androidtv: keydown',
            'key=', key,
            'code=', code,
            'target=', target.tagName, target.className,
            'active=', active.tagName, active.className,
            'isPlaying=', window.__androidTvIsPlaying,
            'menuMode=', window.__androidTvMenuMode,
            'globalUnblock=', window.__androidTvGlobalUnblock
          );
        } catch (err) {}

        // 1) BACK → abilita "tutto libero" (menu alternativo)
        if (isBack) {
          if (!window.__androidTvGlobalUnblock) {
            window.__androidTvGlobalUnblock = true;
            console.log('androidtv: GLOBAL UNBLOCK enabled due to Back');
          }
          // non blocchiamo mai Back
          return;
        }

        // se siamo in globalUnblock, da qui in poi non blocchiamo più nulla
        if (window.__androidTvGlobalUnblock) {
          if (isLeft || isRight) {
            console.log('androidtv: LEFT/RIGHT allowed (globalUnblock=true)');
          }
          return;
        }

        // 2) gestione menu principale (overlay)
        // entra in menuMode: video in play + frecce verticali
        if (!window.__androidTvMenuMode &&
            window.__androidTvIsPlaying &&
            (isUp || isDown)) {
          window.__androidTvMenuMode = true;
          console.log('androidtv: ENTER menu mode, reason=', key);
        }
        // esci da menuMode: mentre sei in menu, premi RIGHT o ENTER
        else if (window.__androidTvMenuMode &&
                 (isRight || isEnter)) {
          window.__androidTvMenuMode = false;
          console.log('androidtv: EXIT menu mode, reason=', key);
          // lasciamo passare questo evento al sito
          return;
        }

        // 3) filtro LEFT/RIGHT

        if (!isLeft && !isRight) {
          // non ci interessa altro
          return;
        }

        // se siamo in menuMode, non blocchiamo le frecce orizzontali
        if (window.__androidTvMenuMode) {
          console.log('androidtv: LEFT/RIGHT allowed (menuMode=true)');
          return;
        }

        // se il video non sta riproducendo, non blocchiamo
        if (!window.__androidTvIsPlaying) {
          console.log('androidtv: LEFT/RIGHT allowed (not playing)');
          return;
        }

        // video in play, menuMode=false, globalUnblock=false → blocchiamo seek del player
        e.preventDefault();
        e.stopPropagation();
        try {
          console.log(
            'androidtv: BLOCKED',
            (isLeft ? 'LEFT' : 'RIGHT'),
            'while video is playing and menuMode=false'
          );
        } catch (err) {}
      }, true);
    })();
""".trimIndent()