document.addEventListener("DOMContentLoaded", () => {
    const form = document.querySelector("#route-form");
    const calculateButton = document.querySelector("#calculate-route-button");
    const routeStatus = document.querySelector("#route-status");
    const results = document.querySelector("#route-results");
    const summary = document.querySelector("#route-summary");
    const legList = document.querySelector("#route-legs");

    form.addEventListener("submit", calculateRoute);

    async function calculateRoute(event) {
        event.preventDefault();
        if (!form.reportValidity()) {
            return;
        }

        clearPreviousResult();
        setLoading(true);

        try {
            const mapAvailable = await window.airportRouteMap?.whenAirportsReady();
            if (!mapAvailable) {
                throw new Error("Airport data must be available before a route can be displayed.");
            }

            const request = buildRequest();
            const response = await fetch("/api/ui/routes/calculate", {
                method: "POST",
                headers: {
                    Accept: "application/json",
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(request)
            });

            if (!response.ok) {
                const problem = await response.json().catch(() => null);
                throw new Error(problem?.detail || "The route could not be calculated.");
            }

            const route = await response.json();
            renderResult(route);
            const airportCodes = window.airportRouteMap.drawRoute(route);
            routeStatus.textContent = `${route.routeType} route ready: ${airportCodes.join(" → ")}`;
            routeStatus.classList.add("success");
            window.routePlannerState = Object.freeze({
                routeType: route.routeType,
                airportCodes: Object.freeze([...airportCodes]),
                legCount: route.legs.length
            });
        } catch (error) {
            routeStatus.textContent = error.message || "The route could not be calculated.";
            routeStatus.classList.add("error");
        } finally {
            setLoading(false);
        }
    }

    function buildRequest() {
        const routeType = form.querySelector('input[name="routeType"]:checked').value;
        const request = {
            originAirportCode: form.elements.originAirportCode.value,
            destinationAirportCode: form.elements.destinationAirportCode.value,
            routeType,
            departureLocalDateTime: null
        };

        if (routeType === "FASTEST") {
            request.departureLocalDateTime = `${form.elements.departureDate.value}T${form.elements.departureTime.value}`;
        }
        return request;
    }

    function clearPreviousResult() {
        window.airportRouteMap?.clearRoute();
        results.hidden = true;
        summary.replaceChildren();
        legList.replaceChildren();
        routeStatus.textContent = "";
        routeStatus.className = "route-status";
        window.routePlannerState = null;
    }

    function setLoading(loading) {
        calculateButton.disabled = loading;
        calculateButton.textContent = loading ? "Calculating route…" : "Calculate route";
        form.setAttribute("aria-busy", String(loading));
        if (loading) {
            routeStatus.textContent = "Calculating route…";
        }
    }

    function renderResult(route) {
        const airportCodes = route.legs.length === 0
                ? [route.originAirportCode]
                : [route.legs[0].originAirportCode, ...route.legs.map(leg => leg.destinationAirportCode)];

        const heading = document.createElement("h4");
        heading.textContent = `${displayRouteType(route.routeType)} route`;
        summary.appendChild(heading);

        const path = document.createElement("p");
        path.className = "route-path";
        path.textContent = airportCodes.join(" → ");
        summary.appendChild(path);

        const metrics = document.createElement("dl");
        metrics.className = "route-metrics";
        appendMetric(metrics, "Distance", `${formatNumber(route.totalDistanceKm)} km`);
        appendMetric(metrics, "Aircraft fuel", `${formatNumber(route.totalFuelLitres)} L`);
        appendMetric(metrics, "Fuel per passenger", `${formatNumber(route.fuelPerPassengerLitres)} L`);
        appendMetric(metrics, "Flight legs", String(route.legs.length));

        if (route.routeType === "FASTEST") {
            appendMetric(metrics, "Total journey", formatDuration(route.totalJourneyMinutes));
            appendMetric(metrics, "Journey start UTC", route.journeyStartUtc);
            appendMetric(metrics, "Journey arrival UTC", route.journeyArrivalUtc);
        }
        summary.appendChild(metrics);

        if (route.legs.length === 0) {
            const message = document.createElement("p");
            message.className = "zero-leg-message";
            message.textContent = "Origin and destination are the same. No flight legs are required.";
            summary.appendChild(message);
        } else {
            route.legs.forEach((leg, index) => legList.appendChild(createLeg(leg, index, route.routeType)));
        }

        results.hidden = false;
    }

    function createLeg(leg, index, routeType) {
        const item = document.createElement("li");
        const article = document.createElement("article");
        article.className = "route-leg";

        const heading = document.createElement("h4");
        heading.textContent = `${index + 1}. ${leg.originAirportCode} → ${leg.destinationAirportCode}`;
        article.appendChild(heading);

        const identity = document.createElement("p");
        identity.className = "leg-identity";
        identity.textContent = `${leg.companyCode} route ${leg.routeNumber} · ${displayDirection(leg.direction)} · aircraft type ${leg.aircraftTypeId}`;
        article.appendChild(identity);

        const metrics = document.createElement("dl");
        metrics.className = "leg-metrics";
        appendMetric(metrics, "Distance", `${formatNumber(leg.distanceKm)} km`);
        appendMetric(metrics, "Aircraft fuel", `${formatNumber(leg.fuelLitres)} L`);
        appendMetric(metrics, "Fuel per passenger", `${formatNumber(leg.fuelPerPassengerLitres)} L`);
        appendMetric(metrics, "Daily scheduled departure", `${leg.scheduledDepartureUtc} UTC`);
        appendMetric(
                metrics,
                "Daily adjusted departure",
                `${leg.adjustedDepartureUtc} UTC${leg.adjustedDepartureDayOffset > 0 ? ` (+${leg.adjustedDepartureDayOffset} day)` : ""}`);
        if (leg.delayMinutes > 0) {
            appendMetric(metrics, "Runway delay", `${leg.delayMinutes} min`);
        }

        if (routeType === "FASTEST") {
            appendMetric(metrics, "Scheduled departure UTC", leg.scheduledDepartureInstant);
            appendMetric(metrics, "Adjusted departure UTC", leg.adjustedDepartureInstant);
            appendMetric(metrics, "Arrival UTC", leg.arrivalInstant);
            appendMetric(metrics, "Waiting time", formatDuration(leg.waitingMinutes));
            appendMetric(metrics, "Flight duration", formatDuration(leg.flightDurationMinutes));
        }
        article.appendChild(metrics);
        item.appendChild(article);
        return item;
    }

    function appendMetric(list, label, value) {
        const term = document.createElement("dt");
        term.textContent = label;
        const description = document.createElement("dd");
        description.textContent = value;
        list.append(term, description);
    }

    function formatNumber(value) {
        return new Intl.NumberFormat("en-US", {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        }).format(value);
    }

    function formatDuration(totalMinutes) {
        if (totalMinutes === null || totalMinutes === undefined) {
            return "Not available";
        }
        const totalSeconds = Math.round(totalMinutes * 60);
        const hours = Math.floor(totalSeconds / 3600);
        const minutes = Math.floor((totalSeconds % 3600) / 60);
        const seconds = totalSeconds % 60;
        return [
            hours > 0 ? `${hours} h` : null,
            `${minutes} min`,
            seconds > 0 ? `${seconds} sec` : null
        ].filter(Boolean).join(" ");
    }

    function displayRouteType(routeType) {
        return routeType.charAt(0) + routeType.slice(1).toLowerCase();
    }

    function displayDirection(direction) {
        return direction.charAt(0) + direction.slice(1).toLowerCase();
    }
});
