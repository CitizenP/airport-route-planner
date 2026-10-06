document.addEventListener("DOMContentLoaded", () => {
    const statusElement = document.querySelector("#airport-status");
    const errorElement = document.querySelector("#airport-error");
    const originSelect = document.querySelector("#origin-airport");
    const destinationSelect = document.querySelector("#destination-airport");
    const departureDate = document.querySelector("#departure-date");
    const departureTime = document.querySelector("#departure-time");
    const routeTypeInputs = document.querySelectorAll('input[name="routeType"]');

    configureFastestFields(routeTypeInputs, departureDate, departureTime);

    if (typeof L === "undefined") {
        showError("The interactive map library could not be loaded. Please check your connection and try again.");
        return;
    }

    const map = L.map("airport-map", {
        minZoom: 2,
        worldCopyJump: true
    }).setView([22, 5], 2);

    L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
        maxZoom: 18,
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap contributors</a>'
    }).addTo(map);

    const airportMarkerIcon = L.icon({
        iconUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png",
        iconRetinaUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png",
        shadowUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png",
        iconSize: [25, 41],
        iconAnchor: [12, 41],
        popupAnchor: [1, -34],
        shadowSize: [41, 41]
    });

    fetch("/api/ui/airports", {headers: {Accept: "application/json"}})
        .then(async response => {
            if (!response.ok) {
                const problem = await response.json().catch(() => null);
                throw new Error(problem?.detail || "Airport data is temporarily unavailable.");
            }
            return response.json();
        })
        .then(airports => renderAirports(map, airports))
        .catch(error => showError(error.message));

    function renderAirports(airportMap, airports) {
        const bounds = [];
        let markerCount = 0;

        clearSelect(originSelect, "Choose an origin airport");
        clearSelect(destinationSelect, "Choose a destination airport");

        for (const airport of airports) {
            const coordinates = [airport.latitude, airport.longitude];
            const markerLabel = `${airport.iataCode} — ${airport.name}`;
            const marker = L.marker(coordinates, {
                icon: airportMarkerIcon,
                title: markerLabel,
                alt: markerLabel
            })
                .bindPopup(createPopup(airport))
                .addTo(airportMap);
            markerCount += 1;
            bounds.push(coordinates);

            const label = `${airport.name} (${airport.iataCode}) — ${airport.city}, ${airport.country}`;
            addOption(originSelect, airport.iataCode, label);
            addOption(destinationSelect, airport.iataCode, label);
        }

        if (bounds.length > 0) {
            airportMap.fitBounds(bounds, {padding: [24, 24], maxZoom: 3});
        }

        originSelect.disabled = false;
        destinationSelect.disabled = false;
        statusElement.textContent = `${airports.length} airports loaded · ${markerCount} map markers`;
        statusElement.dataset.airportCount = String(airports.length);
        statusElement.dataset.markerCount = String(markerCount);
        window.airportMapState = Object.freeze({airportCount: airports.length, markerCount});
    }

    function createPopup(airport) {
        const popup = document.createElement("article");
        popup.className = "airport-popup";

        const heading = document.createElement("h3");
        heading.textContent = airport.name;
        popup.appendChild(heading);

        const code = document.createElement("span");
        code.className = "iata";
        code.textContent = airport.iataCode;
        popup.appendChild(code);

        popup.appendChild(paragraph(`${airport.city}, ${airport.country}`));
        popup.appendChild(paragraph(`${airport.numberOfRunways} ${airport.numberOfRunways === 1 ? "runway" : "runways"}`));
        popup.appendChild(paragraph(airport.formattedUtcOffset || formatUtcOffset(airport.utcOffsetMinutes)));
        return popup;
    }

    function paragraph(text) {
        const element = document.createElement("p");
        element.textContent = text;
        return element;
    }

    function clearSelect(select, prompt) {
        select.replaceChildren();
        addOption(select, "", prompt);
    }

    function addOption(select, value, label) {
        const option = document.createElement("option");
        option.value = value;
        option.textContent = label;
        select.appendChild(option);
    }

    function showError(message) {
        statusElement.textContent = "Airport network unavailable";
        errorElement.textContent = message;
        errorElement.hidden = false;
        originSelect.disabled = true;
        destinationSelect.disabled = true;
    }
});

function configureFastestFields(routeTypeInputs, departureDate, departureTime) {
    const update = () => {
        const fastestSelected = document.querySelector('input[name="routeType"]:checked')?.value === "FASTEST";
        departureDate.disabled = !fastestSelected;
        departureTime.disabled = !fastestSelected;
    };
    routeTypeInputs.forEach(input => input.addEventListener("change", update));
    update();
}

function formatUtcOffset(offsetMinutes) {
    if (offsetMinutes === 0) {
        return "UTC+0";
    }
    const sign = offsetMinutes < 0 ? "-" : "+";
    const absoluteMinutes = Math.abs(offsetMinutes);
    const hours = Math.floor(absoluteMinutes / 60);
    const minutes = absoluteMinutes % 60;
    return minutes === 0
        ? `UTC${sign}${hours}`
        : `UTC${sign}${hours}:${String(minutes).padStart(2, "0")}`;
}
