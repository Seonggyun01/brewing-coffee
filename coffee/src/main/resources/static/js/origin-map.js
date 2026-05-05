(function () {
    const countries = Array.from(document.querySelectorAll('.map-country'));
    const countryPanel = document.querySelector('[data-country-panel]');
    const countryName = countryPanel.querySelector('[data-country-name]');
    const countrySummary = countryPanel.querySelector('[data-country-summary]');
    const countryLink = countryPanel.querySelector('[data-country-link]');
    const countryMore = countryPanel.querySelector('[data-country-more]');
    const countryPanelClose = document.querySelector('[data-country-panel-close]');
    const recentBeans = document.querySelector('[data-recent-beans]');
    const statCountries = document.querySelector('[data-stat="countries"]');
    const statBeans = document.querySelector('[data-stat="beans"]');
    const statBrews = document.querySelector('[data-stat="brews"]');
    const countryMap = new Map();

    function setPanel(country) {
        document.body.classList.add('has-country-panel');
        countryPanel.hidden = false;
        countries.forEach((countryShape) => countryShape.classList.toggle('is-selected', countryShape.id === country.countryCode));
        countryName.textContent = `${country.koreanCountryName} (${country.countryName})`;
        countrySummary.textContent = `경험한 원두 ${country.beanCount}개, 브루잉 기록 ${country.brewRecordCount}개`;
        countryLink.textContent = `${country.koreanCountryName} 원두 보기`;
        countryLink.href = `/coffee-beans?countryCode=${country.countryCode}`;
        countryMore.href = `/coffee-beans?countryCode=${country.countryCode}`;
        renderRecentBeans(country.recentBeans);
    }

    function renderRecentBeans(beans) {
        recentBeans.innerHTML = '';
        if (!beans || beans.length === 0) {
            const empty = document.createElement('p');
            empty.textContent = '아직 등록된 원두가 없습니다.';
            recentBeans.appendChild(empty);
            return;
        }

        beans.forEach((bean) => {
            const link = document.createElement('a');
            link.className = 'origin-bean-card';
            link.href = `/coffee-beans/${bean.id}`;

            const flavorGradient = document.createElement('span');
            flavorGradient.className = 'flavor-gradient origin-bean-card__flavor-gradient';
            flavorGradient.setAttribute('aria-hidden', 'true');
            flavorGradient.setAttribute('style', bean.flavorGradientStyle || 'background: linear-gradient(90deg, #d8c8b5, #efe4d3);');

            const name = document.createElement('strong');
            name.textContent = bean.name;

            const roastery = document.createElement('span');
            roastery.textContent = bean.roastery || '로스터리 미기록';

            const meta = document.createElement('small');
            meta.textContent = `${bean.region || '지역 미기록'} · ${bean.processType || '가공 미기록'}`;

            const notes = document.createElement('small');
            notes.className = 'origin-bean-card__notes';
            notes.textContent = bean.flavorNoteSummary || '향미 미기록';

            link.append(flavorGradient, name, roastery, meta, notes);
            recentBeans.appendChild(link);
        });
    }

    fetch('/api/origin-map/countries')
        .then((response) => response.json())
        .then((data) => {
            statCountries.textContent = data.experiencedCountryCount;
            statBeans.textContent = data.totalBeanCount;
            statBrews.textContent = data.totalBrewRecordCount;

            data.countries.forEach((country) => {
                countryMap.set(country.countryCode, country);
                const shape = document.getElementById(country.countryCode);
                if (shape) {
                    shape.style.fill = country.fillColor;
                    shape.dataset.beanCount = country.beanCount;
                    shape.classList.add('has-records');
                }
            });
        });

    countries.forEach((countryShape) => {
        countryShape.addEventListener('click', () => {
            const country = countryMap.get(countryShape.id);
            if (!country) {
                return;
            }
            setPanel(country);
        });
    });

    countryPanelClose.addEventListener('click', () => {
        document.body.classList.remove('has-country-panel');
        countryPanel.hidden = true;
        countries.forEach((countryShape) => countryShape.classList.remove('is-selected'));
    });
}());
