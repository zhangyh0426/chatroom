(function () {
    var reducedMotion = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    var isMobile = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);

    // ==========================================
    // 1. GLOBAL LENIS SMOOTH SCROLL (Phase 2)
    // ==========================================
    let lenis = null;
    function initSmoothScroll() {
        if (typeof window.Lenis !== 'undefined' && !reducedMotion && !isMobile) {
            lenis = new window.Lenis({
                duration: 1.2,
                easing: function(t) { return Math.min(1, 1.001 - Math.pow(2, -10 * t)); }, 
                direction: 'vertical',
                gestureDirection: 'vertical',
                smooth: true,
                mouseMultiplier: 1,
                smoothTouch: false,
                touchMultiplier: 2,
                infinite: false,
            });

            function raf(time) {
                lenis.raf(time);
                requestAnimationFrame(raf);
            }
            requestAnimationFrame(raf);

            // Connect GSAP ScrollTrigger to Lenis Timeline Sync
            if (typeof gsap !== 'undefined' && typeof ScrollTrigger !== 'undefined') {
                lenis.on('scroll', ScrollTrigger.update);
                gsap.ticker.add(function(time) {
                    lenis.raf(time * 1000);
                });
                gsap.ticker.lagSmoothing(0);
            }
        }
    }

    // ==========================================
    // 2. MAGNETIC CUSTOM CURSOR (Phase 1)
    // ==========================================
    function initCustomCursor() {
        // Only run on desktop devices
        if (isMobile || reducedMotion) return;

        document.documentElement.classList.add('has-custom-cursor');

        var cursor = document.createElement('div');
        cursor.className = 'magic-cursor';
        var follower = document.createElement('div');
        follower.className = 'magic-cursor-follower';
        
        document.body.appendChild(cursor);
        document.body.appendChild(follower);

        var mouseX = window.innerWidth / 2, mouseY = window.innerHeight / 2;
        var followerX = mouseX, followerY = mouseY;
        var cursorX = mouseX, cursorY = mouseY;
        var isHovering = false;

        document.addEventListener('mousemove', function(e) {
            mouseX = e.clientX;
            mouseY = e.clientY;
            
            if (!isHovering && typeof gsap !== 'undefined') {
                gsap.to(cursor, { duration: 0.1, x: mouseX, y: mouseY, ease: "power2.out"});
            }
        });

        // Use GSAP ticker for a buttery, monitor-refresh-rate synchronized lag effect
        if (typeof gsap !== 'undefined') {
            gsap.ticker.add(function() {
                // Spring damping physics for follower
                followerX += (mouseX - followerX) * 0.12;
                followerY += (mouseY - followerY) * 0.12;
                gsap.set(follower, { x: followerX, y: followerY });
                
                if (!isHovering) {
                    cursorX += (mouseX - cursorX) * 0.62;
                    cursorY += (mouseY - cursorY) * 0.62;
                    gsap.set(cursor, { x: cursorX, y: cursorY });
                }
            });
        }

        // Apply Magnetic Pull & Hover Triggers
        var interactives = document.querySelectorAll('a, button, .btn, .interactive-card');
        interactives.forEach(function(el) {
            el.addEventListener('mouseenter', function() {
                isHovering = true;
                follower.classList.add('is-active');
                cursor.classList.add('is-hidden');
                
                // Button organic bloat
                if (typeof gsap !== 'undefined' && el.classList.contains('btn')) {
                    gsap.to(el, { duration: 0.28, scale: 1.02, ease: "power2.out" });
                }
            });
            
            el.addEventListener('mouseleave', function() {
                isHovering = false;
                follower.classList.remove('is-active');
                cursor.classList.remove('is-hidden');
                
                // Return to pure state
                if (typeof gsap !== 'undefined') {
                    gsap.to(el, { duration: 0.4, scale: 1, x: 0, y: 0, ease: "power2.out" });
                }
            });

            // Magnetic snap tracking
            if (el.classList.contains('btn') || el.classList.contains('board-card')) {
                el.addEventListener('mousemove', function(e) {
                    if (typeof gsap === 'undefined') return;
                    var rect = el.getBoundingClientRect();
                    var cx = rect.left + rect.width / 2;
                    var cy = rect.top + rect.height / 2;
                    var dx = e.clientX - cx;
                    var dy = e.clientY - cy;
                    
                    // Physically pull the element towards mouse
                    gsap.to(el, { duration: 0.24, x: dx * 0.06, y: dy * 0.06, ease: "power2.out" });
                    
                    // Snap the follower rigidly to the center of the pulled object
                    mouseX = cx + dx * 0.04;
                    mouseY = cy + dy * 0.04;
                    
                    // Stick the dot exactly to the button center
                    gsap.set(cursor, { x: cx + dx * 0.02, y: cy + dy * 0.02 });
                });
            }
        });
    }

    // ==========================================
    // 3. ENHANCED 3D TILT WITH GSAP (Phase 1)
    // ==========================================
    function initCardTilt() {
        if (reducedMotion) return;
        var cards = document.querySelectorAll('.interactive-card');
        
        cards.forEach(function(card) {
            // Apply glare lighting
            var glare = card.querySelector('.glare');
            if (!glare) {
                glare = document.createElement('div');
                glare.className = 'glare';
                glare.style.cssText = 'position:absolute; inset:0; pointer-events:none; border-radius:inherit; opacity:0; z-index:10; background:radial-gradient(circle at 50% 50%, rgba(255,255,255,0.4) 0%, rgba(255,255,255,0) 60%); transition: opacity 0.4s;';
                card.style.position = card.style.position === 'relative' || card.style.position === 'absolute' ? card.style.position : 'relative';
                card.appendChild(glare);
            }

            card.addEventListener('mousemove', function(e) {
                var rect = card.getBoundingClientRect();
                var x = e.clientX - rect.left;
                var y = e.clientY - rect.top;
                var cx = rect.width / 2;
                var cy = rect.height / 2;
                var dx = (x - cx) / cx; // -1 to 1
                var dy = (y - cy) / cy; // -1 to 1

                if (typeof gsap !== 'undefined') {
                    // Ultra-smooth 3D physical rotation
                    gsap.to(card, {
                        duration: 0.32,
                        rotateX: -dy * 4,
                        rotateY: dx * 4,
                        y: -2,
                        boxShadow: '0 20px 40px rgba(12, 21, 38, 0.14)',
                        ease: "power3.out"
                    });
                    
                    // Internal layer Parallax (e.g. icon floating off board surface)
                    var icon = card.querySelector('.board-icon');
                    if (icon) gsap.to(icon, { duration: 0.32, x: dx * 5, y: dy * 5, ease: "power3.out" });
                    
                    // Light tracking mapping
                    gsap.to(glare, {
                        duration: 0.32,
                        opacity: 0.68,
                        background: 'radial-gradient(circle at ' + x + 'px ' + y + 'px, rgba(255,255,255,0.3) 0%, rgba(255,255,255,0) 60%)'
                    });
                } else {
                    card.style.transform = 'perspective(1000px) rotateX(' + (-dy * 4) + 'deg) rotateY(' + (dx * 4) + 'deg) translateY(-2px)';
                }
            });

            card.addEventListener('mouseleave', function() {
                if (typeof gsap !== 'undefined') {
                    // Damped elastic return to 0 state
                    gsap.to(card, {
                        duration: 0.42, rotateX: 0, rotateY: 0, y: 0,
                        boxShadow: 'var(--shadow-sm)',
                        ease: "power2.out"
                    });
                    var icon = card.querySelector('.board-icon');
                    if (icon) gsap.to(icon, { duration: 0.42, x: 0, y: 0, ease: "power2.out" });
                    gsap.to(glare, { duration: 0.3, opacity: 0 });
                } else {
                    card.style.transform = '';
                }
            });
        });
    }

    // ==========================================
    // 4. HERO PARALLAX & GSAP STORYTELLING (Phase 2)
    // ==========================================
    function initGSAPAnimations() {
        if (typeof gsap === 'undefined' || typeof ScrollTrigger === 'undefined' || reducedMotion) {
            // Fallback for reveals if GSAP failed/blocked
            document.querySelectorAll('[data-reveal]').forEach(el => el.classList.add('is-visible'));
            return;
        }
        
        gsap.registerPlugin(ScrollTrigger);

        // Hero Typography Cinematic reveals (Bienville Style)
        var heroTitle = document.querySelector('.hero-copy h1');
        if (heroTitle && !heroTitle.classList.contains('split-done')) {
            gsap.fromTo(heroTitle, 
                { y: 60, opacity: 0, clipPath: 'polygon(0 0, 100% 0, 100% 0, 0 0)' },
                { y: 0, opacity: 1, clipPath: 'polygon(0 0, 100% 0, 100% 100%, 0 100%)', duration: 1.4, ease: 'expo.out', delay: 0.1 }
            );
        }

        var heroDesc = document.querySelector('.hero-copy p');
        var heroBtns = document.querySelector('.hero-actions');
        if (heroDesc) {
            gsap.fromTo([heroDesc, heroBtns], 
                { y: 25, opacity: 0 },
                { y: 0, opacity: 1, duration: 1.2, stagger: 0.15, ease: 'power3.out', delay: 0.4 }
            );
        }

        // Z-Axis Parallax Flow: Orbs drop down slowly as you scroll down
        var orbs = document.querySelectorAll('.hero-orb');
        orbs.forEach(function(orb, i) {
            gsap.to(orb, {
                y: (i + 1) * 140,
                ease: 'none',
                scrollTrigger: {
                    trigger: '.page-home',
                    start: 'top top',
                    end: 'bottom top',
                    scrub: 1.5 // Adds inertia to parallax
                }
            });
        });

        // Metric Stack Sequence
        var metricCards = document.querySelectorAll('.metric-card');
        if (metricCards.length) {
            gsap.fromTo(metricCards,
                { x: 40, opacity: 0 },
                {
                    x: 0, opacity: 1, duration: 1, stagger: 0.12, ease: "back.out(1.2)", delay: 0.7,
                    scrollTrigger: { trigger: '.metric-stack', start: "top 95%" }
                }
            );
        }

        // Global Scroll Reveals (Replaces old sluggish IntersectionObserver)
        var reveals = document.querySelectorAll('[data-reveal]');
        reveals.forEach(function(el) {
            if (el.classList.contains('metric-card')) return;
            
            var d = parseInt(el.getAttribute('data-delay') || "0") / 1000;
            gsap.fromTo(el,
                { opacity: 0, y: 50 },
                {
                    opacity: 1, y: 0, duration: 1, delay: d, ease: "power3.out",
                    scrollTrigger: {
                        trigger: el,
                        start: "top 85%", // Starts earlier for better flow
                        toggleActions: "play none none reverse"
                    }
                }
            );
        });
        
        // Dynamic Entry for the board cards grouping
        var boardCards = document.querySelectorAll('.board-card');
        if (boardCards.length) {
            gsap.fromTo(boardCards,
                { y: 60, opacity: 0 },
                {
                    y: 0, opacity: 1, duration: 0.8, stagger: 0.08, ease: "power3.out",
                    scrollTrigger: {
                        trigger: '.board-grid',
                        start: "top 80%"
                    }
                }
            );
        }
    }

    // ==========================================
    // Original Utilities Retained
    // ==========================================
    function initHeaderCompact() {
        var header = document.querySelector('.site-header');
        if (!header) return;
        function update() {
            if (window.scrollY > 16) header.classList.add('compact');
            else header.classList.remove('compact');
        }
        update();
        window.addEventListener('scroll', update, { passive: true });
    }

    function initButtonsRipple() {
        var buttons = document.querySelectorAll('.btn');
        buttons.forEach(function(btn) {
            btn.addEventListener('click', function(event) {
                var ripple = document.createElement('span');
                ripple.className = 'ripple';
                var size = Math.max(btn.offsetWidth, btn.offsetHeight);
                ripple.style.width = size + 'px';
                ripple.style.height = size + 'px';
                var rect = btn.getBoundingClientRect();
                ripple.style.left = (event.clientX - rect.left) + 'px';
                ripple.style.top = (event.clientY - rect.top) + 'px';
                var old = btn.querySelector('.ripple');
                if (old) old.remove();
                btn.appendChild(ripple);
                window.setTimeout(function() { ripple.remove(); }, 700);
            });
        });
    }

    function initCountUp() {
        var counters = document.querySelectorAll('[data-count]');
        if (!counters.length) return;
        function animateCounter(el) {
            if (el.getAttribute('data-counted') === '1') return;
            var target = parseInt(el.getAttribute('data-count'), 10);
            if (isNaN(target)) return;
            el.setAttribute('data-counted', '1');
            var duration = 1200, start = performance.now(), from = 0;
            function frame(now) {
                var progress = Math.min((now - start) / duration, 1);
                var eased = 1 - Math.pow(1 - progress, 4); // Quart ease out
                el.textContent = String(Math.round(from + (target - from) * eased));
                if (progress < 1) requestAnimationFrame(frame);
            }
            requestAnimationFrame(frame);
        }

        if (typeof ScrollTrigger !== 'undefined') {
            counters.forEach(function(item) {
                ScrollTrigger.create({
                    trigger: item,
                    start: "top 90%",
                    onEnter: function() { animateCounter(item); }
                });
            });
        } else if ('IntersectionObserver' in window) {
            var observer = new IntersectionObserver(function(entries, obs) {
                entries.forEach(function(entry) {
                    if (entry.isIntersecting) { animateCounter(entry.target); obs.unobserve(entry.target); }
                });
            }, { threshold: 0.45 });
            counters.forEach(function(item) { observer.observe(item); });
        }
    }

    function initRoomCreatePanel() {
        var root = document.querySelector('[data-room-create-root]');
        if (!root) return;

        var toggles = document.querySelectorAll('[data-room-create-toggle][data-target="rooms-create"]');
        var picker = root.querySelector('[data-partition-picker]');
        var modeInput = root.querySelector('[data-partition-mode]');
        var newPartitionGroup = root.querySelector('[data-new-partition-group]');
        var newPartitionInput = root.querySelector('[data-new-partition-input]');

        function setOpen(nextOpen) {
            root.classList.toggle('is-open', nextOpen);
            root.setAttribute('data-open', nextOpen ? 'true' : 'false');
        }

        function syncPartitionFields() {
            if (!modeInput) return;

            var hasPicker = !!picker;
            var nextMode = 'new';
            if (hasPicker && picker.value !== '__NEW__') {
                nextMode = 'existing';
            }
            modeInput.value = nextMode;

            if (newPartitionGroup) {
                newPartitionGroup.classList.toggle('is-hidden', nextMode !== 'new');
            }
            if (newPartitionInput) {
                newPartitionInput.disabled = nextMode !== 'new';
                newPartitionInput.required = nextMode === 'new';
            }
        }

        toggles.forEach(function(toggle) {
            toggle.addEventListener('click', function() {
                setOpen(!root.classList.contains('is-open'));
            });
        });

        if (picker) {
            picker.addEventListener('change', function() {
                syncPartitionFields();
                if (picker.value === '__NEW__') {
                    setOpen(true);
                    if (newPartitionInput) {
                        window.setTimeout(function() { newPartitionInput.focus(); }, 0);
                    }
                }
            });
        }

        setOpen(root.getAttribute('data-open') === 'true');
        syncPartitionFields();
    }

    function onReady(fn) {
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', fn);
        } else {
            fn();
        }
    }

    // Bootstrap Sequence
    onReady(function () {
        document.documentElement.classList.add('motion-ready');
        // Phase 1 + 2 Core Inits
        initSmoothScroll();
        initCustomCursor();
        initGSAPAnimations();
        initCardTilt();
        // Existing UI Utilities
        initHeaderCompact();
        initButtonsRipple();
        initCountUp();
        initRoomCreatePanel();
    });
})();
