(function () {
    const centerX = 75;
    const centerY = 67;
    const radius = 48;

    function point(value, index) {
        const score = Number.isFinite(value) ? Math.min(5, Math.max(0, value)) : 0;
        const angle = (-90 + index * 60) * Math.PI / 180;
        const scaledRadius = radius * score / 5;
        return [
            centerX + Math.cos(angle) * scaledRadius,
            centerY + Math.sin(angle) * scaledRadius
        ];
    }

    document.querySelectorAll('[data-brew-card-taste-chart]').forEach((chart) => {
        const values = (chart.dataset.values || '0,0,0,0,0,0')
            .split(',')
            .slice(0, 6)
            .map(Number);
        while (values.length < 6) {
            values.push(0);
        }

        const polygon = chart.querySelector('[data-brew-card-taste-value]');
        polygon.setAttribute('points', values
            .map((value, index) => point(value, index).map((coordinate) => coordinate.toFixed(2)).join(','))
            .join(' '));
    });

    document.querySelectorAll('[data-rating]').forEach((stars) => {
        const rating = Number(stars.dataset.rating);
        stars.querySelectorAll('span').forEach((star, index) => {
            const fill = Number.isFinite(rating)
                ? Math.min(1, Math.max(0, rating - index)) * 100
                : 0;
            star.style.setProperty('--star-fill', `${fill}%`);
        });
    });
})();
