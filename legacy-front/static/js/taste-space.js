import * as THREE from "three";
import { OrbitControls } from "/vendor/three/addons/controls/OrbitControls.js";

const root = document.querySelector("[data-taste-space]");

if (root) {
    const elements = {
        canvas: root.querySelector("[data-space-canvas]"),
        canvasWrap: root.querySelector("[data-canvas-wrap]"),
        loading: root.querySelector("[data-space-loading]"),
        tooltip: root.querySelector("[data-space-tooltip]"),
        count: root.querySelector("[data-space-count]"),
        empty: root.querySelector("[data-space-empty]"),
        profilePanel: root.querySelector("[data-profile-panel]"),
        profileTitle: root.querySelector("[data-profile-title]"),
        profileDescription: root.querySelector("[data-profile-description]"),
        panelEyebrow: root.querySelector("[data-panel-eyebrow]"),
        panelTitle: root.querySelector("[data-panel-title]"),
        panelSubtitle: root.querySelector("[data-panel-subtitle]"),
        panelNotes: root.querySelector("[data-panel-notes]"),
        panelRating: root.querySelector("[data-panel-rating]"),
        panelOrigin: root.querySelector("[data-panel-origin]"),
        panelProcess: root.querySelector("[data-panel-process]"),
        panelBrews: root.querySelector("[data-panel-brews]"),
        panelClosest: root.querySelector("[data-panel-closest]"),
        closestRow: root.querySelector("[data-closest-row]"),
        detailEyebrow: root.querySelector("[data-detail-eyebrow]"),
        detailConfidence: root.querySelector("[data-detail-confidence]"),
        labelRating: root.querySelector("[data-label-rating]"),
        labelOrigin: root.querySelector("[data-label-origin]"),
        labelProcess: root.querySelector("[data-label-process]"),
        labelBrews: root.querySelector("[data-label-brews]"),
        panelLink: root.querySelector("[data-panel-link]"),
        list: root.querySelector("[data-space-list]"),
        reset: root.querySelector("[data-reset-view]"),
        rebuild: root.querySelector("[data-rebuild-space]")
    };

    let renderer;
    let scene;
    let camera;
    let controls;
    let raycaster;
    let pointer;
    let pointMeshes = [];
    let selectedMesh = null;
    let payload = null;
    const gradientTextures = new Map();

    const load = async (force = false) => {
        showLoading(force ? "취향 공간을 다시 계산하고 있어요" : "취향 공간을 불러오고 있어요");
        try {
            let response = await fetch("/api/taste-space", {headers: {Accept: "application/json"}});
            payload = await response.json();
            if (force || payload.state === "EMPTY" && payload.beanCount > 0 || payload.state === "STALE") {
                response = await rebuild();
                payload = await response.json();
            }
            if (!response.ok) {
                throw new Error(payload.message || "취향 공간을 만들지 못했습니다.");
            }
            render(payload);
        } catch (error) {
            showError(error.message);
        }
    };

    const rebuild = () => {
        const headers = {Accept: "application/json"};
        const token = document.querySelector('meta[name="csrf-token"]')?.content;
        const header = document.querySelector('meta[name="csrf-header"]')?.content;
        if (token && header) headers[header] = token;
        return fetch("/api/taste-space/rebuild", {method: "POST", headers});
    };

    const render = (data) => {
        elements.loading.hidden = true;
        elements.count.textContent = `나의 기록 ${data.beanCount}개로 만든 공간`;
        renderProfile(data.profile, data.beanCount, data.points);
        renderList(data.points);
        if (data.points.length === 0) {
            elements.empty.hidden = false;
            elements.profilePanel.hidden = true;
            return;
        }
        elements.empty.hidden = true;
        elements.profilePanel.hidden = false;
        if (!initScene()) return;
        drawPoints(data.points);
    };

    const initScene = () => {
        if (renderer) return true;
        try {
            renderer = new THREE.WebGLRenderer({canvas: elements.canvas, antialias: true, alpha: false});
        } catch (error) {
            showError("이 브라우저에서는 3D 화면을 표시할 수 없습니다. 아래 원두 목록을 이용해주세요.");
            return false;
        }
        renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
        renderer.setClearColor(0x05070d, 1);
        renderer.outputColorSpace = THREE.SRGBColorSpace;

        scene = new THREE.Scene();
        scene.fog = new THREE.FogExp2(0x05070d, 0.035);
        camera = new THREE.PerspectiveCamera(45, 1, 0.1, 100);
        camera.position.set(0, 1.6, 6.8);
        controls = new OrbitControls(camera, elements.canvas);
        controls.enableDamping = true;
        controls.dampingFactor = 0.06;
        controls.minDistance = 3.2;
        controls.maxDistance = 11;
        controls.autoRotate = true;
        controls.autoRotateSpeed = 0.22;

        scene.add(new THREE.AmbientLight(0xaeb9d8, 1.25));
        const key = new THREE.DirectionalLight(0xffffff, 2.4);
        key.position.set(3, 4, 5);
        scene.add(key);
        const rim = new THREE.PointLight(0x6676b8, 1.8, 18);
        rim.position.set(-4, 2, -3);
        scene.add(rim);
        addStarField();
        addGuides();

        raycaster = new THREE.Raycaster();
        pointer = new THREE.Vector2();
        elements.canvas.addEventListener("pointermove", onPointerMove);
        elements.canvas.addEventListener("click", onClick);
        elements.canvas.addEventListener("pointerdown", () => controls.autoRotate = false);
        window.addEventListener("resize", resize);
        resize();
        animate();
        return true;
    };

    const addStarField = () => {
        const starCanvas = document.createElement("canvas");
        starCanvas.width = 16;
        starCanvas.height = 16;
        const starContext = starCanvas.getContext("2d");
        starContext.fillStyle = "#ffffff";
        starContext.beginPath();
        starContext.arc(8, 8, 6.4, 0, Math.PI * 2);
        starContext.fill();
        const starTexture = new THREE.CanvasTexture(starCanvas);
        starTexture.colorSpace = THREE.SRGBColorSpace;

        const createLayer = (count, minimumRadius, radiusRange, size, color) => {
            const positions = new Float32Array(count * 3);
            for (let index = 0; index < count; index++) {
                const distance = minimumRadius + Math.random() * radiusRange;
                const theta = Math.random() * Math.PI * 2;
                const phi = Math.acos(2 * Math.random() - 1);
                positions[index * 3] = distance * Math.sin(phi) * Math.cos(theta);
                positions[index * 3 + 1] = distance * Math.cos(phi);
                positions[index * 3 + 2] = distance * Math.sin(phi) * Math.sin(theta);
            }
            const geometry = new THREE.BufferGeometry();
            geometry.setAttribute("position", new THREE.BufferAttribute(positions, 3));
            const material = new THREE.PointsMaterial({
                color,
                size,
                map: starTexture,
                alphaTest: 0.5,
                sizeAttenuation: false,
                depthWrite: false
            });
            const stars = new THREE.Points(geometry, material);
            stars.renderOrder = -10;
            scene.add(stars);
        };
        createLayer(900, 22, 6, 1.5, 0x8792ad);
        createLayer(180, 24, 5, 2.4, 0xd8def0);
    };

    const addGuides = () => {
        const material = new THREE.LineBasicMaterial({color: 0x777c82, transparent: true, opacity: 0.2});
        [1.35, 2.1].forEach((radius) => {
            const curve = new THREE.EllipseCurve(0, 0, radius, radius * 0.58, 0, Math.PI * 2);
            const points = curve.getPoints(96).map(point => new THREE.Vector3(point.x, -0.8, point.y));
            scene.add(new THREE.LineLoop(new THREE.BufferGeometry().setFromPoints(points), material));
        });
        const axisMaterial = new THREE.LineBasicMaterial({color: 0x777c82, transparent: true, opacity: 0.1});
        [[-2.5, 0, 0, 2.5, 0, 0], [0, -2.1, 0, 0, 2.1, 0], [0, 0, -2.5, 0, 0, 2.5]].forEach(values => {
            scene.add(new THREE.Line(new THREE.BufferGeometry().setFromPoints([
                new THREE.Vector3(values[0], values[1], values[2]),
                new THREE.Vector3(values[3], values[4], values[5])
            ]), axisMaterial));
        });
    };

    const drawPoints = (points) => {
        pointMeshes.forEach(mesh => scene.remove(mesh));
        pointMeshes = [];
        const geometry = new THREE.SphereGeometry(0.11, 28, 20);
        points.forEach((point) => {
            const group = new THREE.Group();
            const gradientTexture = createGradientTexture(point.colors?.length ? point.colors : [point.color]);
            const material = new THREE.MeshStandardMaterial({
                color: 0xffffff,
                map: gradientTexture,
                emissive: new THREE.Color(point.color),
                emissiveIntensity: 0.12,
                roughness: 0.36,
                metalness: 0.06
            });
            const sphere = new THREE.Mesh(geometry, material);
            const halo = new THREE.Mesh(
                new THREE.SphereGeometry(0.19, 24, 16),
                new THREE.MeshBasicMaterial({color: new THREE.Color(point.color), transparent: true, opacity: 0.1})
            );
            group.add(sphere, halo);
            group.position.set(point.x * 2.15, point.y * 1.75, point.z * 2.15);
            group.userData.point = point;
            group.userData.halo = halo;
            scene.add(group);
            pointMeshes.push(group);
        });
    };

    const createGradientTexture = (sourceColors) => {
        const colors = [...new Set(sourceColors.filter(Boolean))].sort((left, right) => colorHue(left) - colorHue(right));
        const normalizedColors = colors.length === 1
            ? [shadeColor(colors[0], 22), colors[0], shadeColor(colors[0], -18)]
            : colors;
        const key = normalizedColors.join("|");
        if (gradientTextures.has(key)) return gradientTextures.get(key);

        const canvas = document.createElement("canvas");
        canvas.width = 256;
        canvas.height = 128;
        const context = canvas.getContext("2d");
        const gradient = context.createLinearGradient(0, 0, canvas.width, canvas.height * 0.28);
        normalizedColors.forEach((color, index) => {
            gradient.addColorStop(index / Math.max(normalizedColors.length - 1, 1), color);
        });
        context.fillStyle = gradient;
        context.fillRect(0, 0, canvas.width, canvas.height);

        const highlight = context.createRadialGradient(82, 32, 2, 82, 32, 82);
        highlight.addColorStop(0, "rgba(255,255,255,0.42)");
        highlight.addColorStop(0.38, "rgba(255,255,255,0.08)");
        highlight.addColorStop(1, "rgba(255,255,255,0)");
        context.fillStyle = highlight;
        context.fillRect(0, 0, canvas.width, canvas.height);

        const texture = new THREE.CanvasTexture(canvas);
        texture.colorSpace = THREE.SRGBColorSpace;
        texture.wrapS = THREE.RepeatWrapping;
        gradientTextures.set(key, texture);
        return texture;
    };

    const colorHue = (hex) => {
        const value = Number.parseInt(hex.replace("#", ""), 16);
        const red = ((value >> 16) & 255) / 255;
        const green = ((value >> 8) & 255) / 255;
        const blue = (value & 255) / 255;
        const maximum = Math.max(red, green, blue);
        const minimum = Math.min(red, green, blue);
        const delta = maximum - minimum;
        if (delta === 0) return 0;
        if (maximum === red) return ((green - blue) / delta + 6) % 6;
        if (maximum === green) return (blue - red) / delta + 2;
        return (red - green) / delta + 4;
    };

    const shadeColor = (hex, amount) => {
        const value = Number.parseInt(hex.replace("#", ""), 16);
        const channel = (shift) => Math.max(0, Math.min(255, ((value >> shift) & 255) + amount));
        return `#${[channel(16), channel(8), channel(0)]
            .map(item => item.toString(16).padStart(2, "0"))
            .join("")}`;
    };

    const renderProfile = (profile, beanCount, points) => {
        elements.panelEyebrow.textContent = "나의 원두 기록";
        elements.panelTitle.textContent = "나의 취향 분석";
        elements.panelSubtitle.textContent = `${beanCount}개 원두와 ${profile.totalBrewCount}회 브루잉 기록을 바탕으로 분석했어요.`;
        elements.profileTitle.textContent = profile.title;
        elements.profileDescription.textContent = profile.description;
        renderChips(profile.dominantNotes, colorsForNotes(profile.dominantNotes, points));
        elements.detailEyebrow.textContent = "선호 향미";
        elements.detailConfidence.textContent = `분석 신뢰도 · ${profile.confidenceLabel}`;
        elements.labelRating.textContent = "평균 만족도";
        elements.labelOrigin.textContent = "선호 산지";
        elements.labelProcess.textContent = "선호 가공";
        elements.labelBrews.textContent = "분석한 브루잉";
        elements.panelRating.textContent = profile.averageRating == null ? "-" : `${profile.averageRating.toFixed(1)} / 5.0`;
        elements.panelOrigin.textContent = profile.preferredOrigin || "기록이 더 필요해요";
        elements.panelProcess.textContent = profile.preferredProcess || "기록이 더 필요해요";
        elements.panelBrews.textContent = `${profile.totalBrewCount}회 · 평점 원두 ${profile.ratedBeanCount}개`;
        elements.closestRow.hidden = true;
        elements.panelLink.hidden = true;
    };

    const renderBean = (point) => {
        elements.panelEyebrow.textContent = point.country || "SELECTED BEAN";
        elements.panelTitle.textContent = point.name;
        elements.panelSubtitle.textContent = [point.roastery, formatProcess(point.process), point.status].filter(Boolean).join(" · ");
        elements.profileTitle.textContent = point.flavorNotes.length ? point.flavorNotes.slice(0, 3).join(" · ") : "기록된 향미를 살펴보세요";
        elements.profileDescription.textContent = "공간에서 가까운 점일수록 기록한 향미와 경험의 문맥이 닮아 있습니다.";
        renderChips(point.flavorNotes, point.colors);
        elements.detailEyebrow.textContent = "원두 정보";
        elements.detailConfidence.textContent = "";
        elements.labelRating.textContent = "평균 만족도";
        elements.labelOrigin.textContent = "산지";
        elements.labelProcess.textContent = "가공 방식";
        elements.labelBrews.textContent = "브루잉 기록";
        elements.panelRating.textContent = point.averageRating == null ? "-" : `${point.averageRating.toFixed(1)} / 5.0`;
        elements.panelOrigin.textContent = point.country || "-";
        elements.panelProcess.textContent = formatProcess(point.process) || "-";
        elements.panelBrews.textContent = `${point.brewCount}회`;
        elements.panelClosest.textContent = point.closestBeanName ? `${point.closestBeanName} · ${Math.round(point.similarity * 100)}%` : "-";
        elements.closestRow.hidden = false;
        elements.panelLink.href = point.detailUrl;
        elements.panelLink.hidden = false;
    };

    const renderChips = (notes, colors = []) => {
        elements.panelNotes.replaceChildren(...(notes || []).slice(0, 8).map((note, index) => {
            const chip = document.createElement("span");
            chip.textContent = note;
            const color = colors[index];
            if (color) {
                chip.style.backgroundColor = colorWithAlpha(color, 0.2);
                chip.style.borderColor = colorWithAlpha(color, 0.52);
            }
            return chip;
        }));
    };

    const colorsForNotes = (notes, points) => (notes || []).map(note => {
        for (const point of points || []) {
            const noteIndex = point.flavorNotes.indexOf(note);
            if (noteIndex >= 0) return point.colors?.[noteIndex] || point.color;
        }
        return null;
    });

    const colorWithAlpha = (hex, alpha) => {
        const normalized = hex.replace("#", "");
        if (!/^[0-9a-f]{6}$/i.test(normalized)) return hex;
        const value = Number.parseInt(normalized, 16);
        return `rgba(${(value >> 16) & 255}, ${(value >> 8) & 255}, ${value & 255}, ${alpha})`;
    };

    const renderList = (points) => {
        elements.list.replaceChildren(...points.map(point => {
            const item = document.createElement("li");
            const link = document.createElement("a");
            link.href = point.detailUrl;
            link.textContent = `${point.name}${point.roastery ? ` · ${point.roastery}` : ""}`;
            item.append(link);
            return item;
        }));
    };

    const onPointerMove = (event) => {
        const mesh = intersect(event);
        pointMeshes.forEach(point => point.scale.setScalar(point === selectedMesh ? 1.3 : 1));
        if (!mesh) {
            elements.tooltip.hidden = true;
            return;
        }
        mesh.scale.setScalar(1.3);
        elements.tooltip.textContent = mesh.userData.point.name;
        elements.tooltip.hidden = false;
        const rect = elements.canvasWrap.getBoundingClientRect();
        elements.tooltip.style.left = `${event.clientX - rect.left + 12}px`;
        elements.tooltip.style.top = `${event.clientY - rect.top + 12}px`;
    };

    const onClick = (event) => {
        const mesh = intersect(event);
        if (!mesh) return;
        selectedMesh = mesh;
        controls.autoRotate = false;
        pointMeshes.forEach(point => {
            point.scale.setScalar(point === mesh ? 1.35 : 1);
            point.userData.halo.material.opacity = point === mesh ? 0.3 : 0.1;
        });
        renderBean(mesh.userData.point);
    };

    const intersect = (event) => {
        if (!raycaster || !camera) return null;
        const rect = elements.canvas.getBoundingClientRect();
        pointer.x = ((event.clientX - rect.left) / rect.width) * 2 - 1;
        pointer.y = -((event.clientY - rect.top) / rect.height) * 2 + 1;
        raycaster.setFromCamera(pointer, camera);
        const spheres = pointMeshes.map(group => group.children[0]);
        const hit = raycaster.intersectObjects(spheres, false)[0];
        return hit ? hit.object.parent : null;
    };

    const resize = () => {
        if (!renderer) return;
        const width = elements.canvasWrap.clientWidth;
        const height = elements.canvasWrap.clientHeight;
        renderer.setSize(width, height, false);
        camera.aspect = width / Math.max(height, 1);
        camera.updateProjectionMatrix();
    };

    const animate = () => {
        controls.update();
        renderer.render(scene, camera);
        requestAnimationFrame(animate);
    };

    const resetView = () => {
        if (!camera || !controls) return;
        camera.position.set(0, 1.6, 6.8);
        controls.target.set(0, 0, 0);
        controls.autoRotate = true;
        selectedMesh = null;
        pointMeshes.forEach(point => {
            point.scale.setScalar(1);
            point.userData.halo.material.opacity = 0.1;
        });
        renderProfile(payload.profile, payload.beanCount, payload.points);
    };

    const showLoading = (title) => {
        elements.loading.hidden = false;
        elements.loading.querySelector("strong").textContent = title;
    };

    const showError = (message) => {
        elements.loading.hidden = false;
        elements.loading.querySelector("span").hidden = true;
        elements.loading.querySelector("strong").textContent = "공간을 불러오지 못했어요";
        elements.loading.querySelector("p").textContent = message;
    };

    const formatProcess = (value) => ({
        WASHED: "워시드", NATURAL: "내추럴", HONEY: "허니", ANAEROBIC: "무산소 발효",
        CARBONIC_MACERATION: "카보닉 메서레이션", DECAF: "디카페인", OTHER: "기타"
    })[value] || value;

    elements.reset.addEventListener("click", resetView);
    elements.rebuild.addEventListener("click", () => load(true));
    load();
}
