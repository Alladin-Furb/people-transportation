const TRACKING_API_URL = window.CONFIRMACAO_API_URL || 'http://localhost:3001';
const POLLING_INTERVAL_MS = 5000;

let trackingMap;
let vanMarker;
let selectedStudent = null;
let latestVanLocation = null;
let studentMarkers = new Map();
let refreshTimer = null;
let hasFittedMap = false;

document.addEventListener('DOMContentLoaded', () => {
    const dateInput = document.getElementById('monitorDate');
    dateInput.value = getTodayDate();

    initializeTrackingMap();
    document.getElementById('refreshButton').addEventListener('click', refreshMonitoringData);
    dateInput.addEventListener('change', () => {
        hasFittedMap = false;
        selectedStudent = null;
        refreshMonitoringData();
    });

    refreshMonitoringData();
    refreshTimer = window.setInterval(refreshMonitoringData, POLLING_INTERVAL_MS);
});

window.addEventListener('beforeunload', () => {
    if (refreshTimer) {
        window.clearInterval(refreshTimer);
    }
});

function getTodayDate() {
    const today = new Date();
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, '0');
    const day = String(today.getDate()).padStart(2, '0');

    return `${year}-${month}-${day}`;
}

function initializeTrackingMap() {
    trackingMap = L.map('trackingMap').setView([-26.9194, -49.0661], 13);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; OpenStreetMap contributors'
    }).addTo(trackingMap);
}

async function refreshMonitoringData() {
    setSyncStatus('Atualizando...');

    try {
        const [presenceResponse, vanResponse] = await Promise.all([
            fetch(`${TRACKING_API_URL}/api/presencas/monitoramento?data=${getSelectedDate()}`),
            fetch(`${TRACKING_API_URL}/api/vans/localizacao/tempo-real`)
        ]);

        if (!presenceResponse.ok) {
            throw new Error('Nao foi possivel carregar as confirmacoes.');
        }

        if (!vanResponse.ok) {
            throw new Error('Nao foi possivel carregar a localizacao da van.');
        }

        const presenceData = await presenceResponse.json();
        latestVanLocation = await vanResponse.json();

        renderPresenceRecords(presenceData.presencas || []);
        renderVan(latestVanLocation);
        updateSummary(presenceData.presencas || []);
        updateSelectedStudentPanel();
        setSyncStatus('Online');
    } catch (error) {
        setSyncStatus('Falha na API');
        document.getElementById('selectedStudent').textContent = error.message || 'Erro ao sincronizar monitoramento.';
    }
}

function getSelectedDate() {
    return document.getElementById('monitorDate').value || getTodayDate();
}

function renderPresenceRecords(students) {
    const list = document.getElementById('studentList');
    studentMarkers.forEach((marker) => trackingMap.removeLayer(marker));
    studentMarkers = new Map();

    if (students.length === 0) {
        list.innerHTML = '<div class="empty-state">Nenhum registro encontrado na tabela de confirmacao para esta data.</div>';
        selectedStudent = null;
        return;
    }

    list.innerHTML = students.map((student, index) => createStudentItem(student, index)).join('');

    students.forEach((student, index) => {
        if (!hasValidCoordinates(student)) {
            return;
        }

        const marker = L.marker([student.latitude, student.longitude], {
            icon: createMarkerIcon('student')
        })
            .addTo(trackingMap)
            .bindPopup(createPresencePopup(student));

        marker.on('click', () => selectStudent(student, index, true));
        studentMarkers.set(index, marker);
    });

    list.querySelectorAll('.student-item').forEach((button) => {
        button.addEventListener('click', () => {
            const index = Number(button.dataset.index);
            selectStudent(students[index], index, true);
        });
    });

    const selectedIndex = selectedStudent
        ? students.findIndex((student) => student.nomeAluno === selectedStudent.nomeAluno)
        : -1;

    selectStudent(students[selectedIndex >= 0 ? selectedIndex : 0], selectedIndex >= 0 ? selectedIndex : 0, false);

    if (!hasFittedMap) {
        fitMapToVisiblePoints(students);
        hasFittedMap = true;
    }
}

function createStudentItem(student, index) {
    const confirmedAt = formatDateTime(student.dataHoraPreConfirmacao);
    const effectiveAt = student.dataHoraConfEfetiva || '--:--';
    const statusClass = student.alunoConfirmouEfetivacao ? 'is-effective' : 'is-pending';
    const coordinatesLabel = hasValidCoordinates(student)
        ? `Lat ${Number(student.latitude).toFixed(5)}, Lng ${Number(student.longitude).toFixed(5)}`
        : 'Sem coordenada valida em LocalEmbarque';

    return `
        <button class="student-item ${statusClass}" type="button" data-index="${index}">
            <strong>${escapeHtml(student.nomeAluno)}</strong>
            <span>${formatTripType(student.tipoDeslocamento)} | Pre-confirmado as ${confirmedAt}</span>
            <span>Efetivacao: ${student.alunoConfirmouEfetivacao ? `sim, as ${effectiveAt}` : 'pendente'}</span>
            <span>Empresa: ${escapeHtml(student.empresaTransporte || 'Nao informada')}</span>
            <span>${coordinatesLabel}</span>
        </button>
    `;
}

function selectStudent(student, index, shouldFocusMap = true) {
    selectedStudent = student;
    document.querySelectorAll('.student-item').forEach((item) => item.classList.remove('is-selected'));

    const selectedButton = document.querySelector(`.student-item[data-index="${index}"]`);
    if (selectedButton) {
        selectedButton.classList.add('is-selected');
    }

    const marker = studentMarkers.get(index);
    if (!hasValidCoordinates(student)) {
        updateSelectedStudentPanel();
        return;
    }

    if (marker && shouldFocusMap) {
        marker.openPopup();
        trackingMap.setView([student.latitude, student.longitude], Math.max(trackingMap.getZoom(), 15));
    }

    updateSelectedStudentPanel();
}

function renderVan(vanLocation) {
    if (!vanLocation || vanLocation.latitude === undefined || vanLocation.longitude === undefined) {
        document.getElementById('vanStatus').textContent = 'Sem sinal';
        return;
    }

    const position = [vanLocation.latitude, vanLocation.longitude];

    if (!vanMarker) {
        vanMarker = L.marker(position, {
            icon: createMarkerIcon('van')
        }).addTo(trackingMap);
    } else {
        vanMarker.setLatLng(position);
    }

    vanMarker.bindPopup(`<strong>${escapeHtml(vanLocation.idVan || 'Van')}</strong><br>${Number(vanLocation.velocidadeKmh || 0).toFixed(1)} km/h`);
    document.getElementById('vanStatus').textContent = vanLocation.idVan || 'Online';
}

function updateSummary(students) {
    document.getElementById('recordsCount').textContent = students.length;
    document.getElementById('mappedStudentsCount').textContent = students.filter(hasValidCoordinates).length;
    document.getElementById('effectiveCount').textContent = students.filter((student) => student.alunoConfirmouEfetivacao).length;
    document.getElementById('lastUpdate').textContent = new Date().toLocaleTimeString('pt-BR', {
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
    });
}

function updateSelectedStudentPanel() {
    const panel = document.getElementById('selectedStudent');

    if (!selectedStudent) {
        panel.textContent = 'Selecione um registro da tabela para ver os detalhes.';
        return;
    }

    if (!hasValidCoordinates(selectedStudent)) {
        panel.innerHTML = `
            <strong>${escapeHtml(selectedStudent.nomeAluno)}</strong>
            <span>Registro encontrado na tabela, mas o campo LocalEmbarque nao possui coordenadas validas para o mapa.</span>
            <span>Status: ${formatPresenceStatus(selectedStudent)}.</span>
        `;
        return;
    }

    if (!latestVanLocation) {
        panel.textContent = `${selectedStudent.nomeAluno}: aguardando localizacao da van.`;
        return;
    }

    const distanceMeters = calculateDistanceMeters(
        { latitude: selectedStudent.latitude, longitude: selectedStudent.longitude },
        { latitude: latestVanLocation.latitude, longitude: latestVanLocation.longitude }
    );

    panel.innerHTML = `
        <strong>${escapeHtml(selectedStudent.nomeAluno)}</strong>
        <span>${formatPresenceStatus(selectedStudent)} | ${formatTripType(selectedStudent.tipoDeslocamento)}</span>
        <span>LocalEmbarque: ${escapeHtml(selectedStudent.localEmbarque)}</span>
        <span>Distancia ate a van: ${formatDistance(distanceMeters)}</span>
        <span>Van atualizada em ${formatDateTime(latestVanLocation.atualizadoEm)}.</span>
    `;
}

function fitMapToVisiblePoints(students) {
    const points = students
        .filter(hasValidCoordinates)
        .map((student) => [student.latitude, student.longitude]);

    if (latestVanLocation) {
        points.push([latestVanLocation.latitude, latestVanLocation.longitude]);
    }

    if (points.length > 1) {
        trackingMap.fitBounds(points, { padding: [40, 40], maxZoom: 15 });
    } else if (points.length === 1) {
        trackingMap.setView(points[0], 15);
    }
}

function hasValidCoordinates(student) {
    return Number.isFinite(Number(student.latitude)) && Number.isFinite(Number(student.longitude));
}

function createPresencePopup(student) {
    return `
        <strong>${escapeHtml(student.nomeAluno)}</strong><br>
        ${formatTripType(student.tipoDeslocamento)}<br>
        ${formatPresenceStatus(student)}
    `;
}

function createMarkerIcon(type) {
    return L.divIcon({
        className: '',
        html: `<div class="marker-dot ${type}"></div>`,
        iconSize: type === 'van' ? [24, 24] : [18, 18],
        iconAnchor: type === 'van' ? [12, 12] : [9, 9]
    });
}

function calculateDistanceMeters(origin, destination) {
    const earthRadiusMeters = 6371000;
    const toRadians = (degrees) => degrees * (Math.PI / 180);
    const deltaLatitude = toRadians(destination.latitude - origin.latitude);
    const deltaLongitude = toRadians(destination.longitude - origin.longitude);
    const originLatitude = toRadians(origin.latitude);
    const destinationLatitude = toRadians(destination.latitude);

    const a =
        Math.sin(deltaLatitude / 2) ** 2 +
        Math.cos(originLatitude) *
            Math.cos(destinationLatitude) *
            Math.sin(deltaLongitude / 2) ** 2;
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    return earthRadiusMeters * c;
}

function formatDistance(distanceMeters) {
    if (distanceMeters >= 1000) {
        return `${(distanceMeters / 1000).toFixed(2)} km`;
    }

    return `${Math.round(distanceMeters)} m`;
}

function formatTripType(type) {
    const labels = {
        VOU_E_VOLTO: 'Vou e volto',
        APENAS_VOLTO: 'Apenas volto',
        APENAS_VOU: 'Apenas vou'
    };

    return labels[type] || 'Deslocamento nao informado';
}

function formatDateTime(value) {
    if (!value) return '--:--';

    return new Date(value).toLocaleTimeString('pt-BR', {
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
    });
}

function formatPresenceStatus(student) {
    if (student.alunoConfirmouEfetivacao) {
        return `Efetivada as ${student.dataHoraConfEfetiva || '--:--'}`;
    }

    if (student.confirmacao) {
        return 'Pre-confirmada, aguardando efetivacao';
    }

    return 'Nao confirmada';
}

function setSyncStatus(message) {
    document.getElementById('syncStatus').textContent = message;
}

function escapeHtml(value) {
    return String(value || '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}
