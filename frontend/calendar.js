// Calendario academico universal

const API_URL = 'http://localhost:3001/api';

let currentDate = new Date();
let calendarData = {};
document.addEventListener('DOMContentLoaded', function() {
    initializeCalendar();
});

async function initializeCalendar() {
    setTodayDate();
    await loadCalendar();
}

async function checkDatabaseConnection() {
    try {
        const response = await fetch(`${API_URL}/health`);
        return response.ok;
    } catch (error) {
        return false;
    }
}

function setCalendarStatus(message, type = '') {
    const status = document.getElementById('calendarStatus');

    if (!status) return;

    status.textContent = message;
    status.className = type ? `calendar-status ${type}` : 'calendar-status';
}

function setTodayDate() {
    const today = new Date();
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, '0');
    document.getElementById('monthYear').value = `${year}-${month}`;
}

function isStandByDay(dayData) {
    return dayData.DiaUtil === false;
}

function getDayHighlight(dayData) {
    const description = `${dayData.Descricao || ''} ${dayData.Tipo || ''}`.toLowerCase();

    if (description.includes('ferias') || description.includes('férias') || description.includes('recesso')) {
        return { className: 'ferias', label: 'Ferias' };
    }

    if (dayData.Feriado) {
        return { className: 'feriado', label: 'Feriado' };
    }

    if (dayData.PontoFacultativo) {
        return { className: 'ponto-facultativo', label: 'Ponto facult.' };
    }

    return null;
}

function normalizeDateKey(value) {
    if (!value) return '';

    if (typeof value === 'string') {
        const match = value.match(/^\d{4}-\d{2}-\d{2}/);
        if (match) return match[0];
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return '';
    }

    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');

    return `${year}-${month}-${day}`;
}

async function loadCalendar() {
    const monthYearInput = document.getElementById('monthYear').value;

    if (monthYearInput) {
        const [year, month] = monthYearInput.split('-');
        currentDate = new Date(year, parseInt(month, 10) - 1, 1);
    }

    await loadCalendarFromDatabase();

    renderCalendar();
    updateSummary();
}

async function loadCalendarFromDatabase() {
    try {
        const year = currentDate.getFullYear();
        const month = String(currentDate.getMonth() + 1).padStart(2, '0');
        const response = await fetch(`${API_URL}/calendar?mes=${year}-${month}`);

        if (!response.ok) {
            console.error('Erro ao carregar calendario:', response.status);
            setCalendarStatus('Nao foi possivel carregar o calendario do banco de dados.', 'error');
            calendarData = {};
            return;
        }

        const data = await response.json();
        calendarData = {};

        data.forEach((item) => {
            const dateKey = normalizeDateKey(item.Data);

            if (!dateKey) return;

            calendarData[dateKey] = {
                DiaUtil: item.DiaUtil,
                DiaSemana: item.DiaSemana,
                DataFormatada: item.DataFormatada,
                Feriado: item.Feriado,
                PontoFacultativo: item.PontoFacultativo,
                Descricao: item.Descricao,
                Tipo: item.Tipo,
            };
        });

        if (data.length === 0) {
            setCalendarStatus('Nenhum dia encontrado para este periodo.', 'warning');
        } else {
            setCalendarStatus('');
        }
    } catch (error) {
        console.error('Erro ao carregar do banco:', error);
        setCalendarStatus('Nao foi possivel conectar na API do calendario. Verifique se o backend esta rodando na porta 3001.', 'error');
        calendarData = {};
    }
}

function renderCalendar() {
    const year = currentDate.getFullYear();
    const month = currentDate.getMonth();
    const monthNames = [
        'Janeiro', 'Fevereiro', 'Marco', 'Abril', 'Maio', 'Junho',
        'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'
    ];

    document.getElementById('currentMonth').textContent = `${monthNames[month]} de ${year}`;

    const firstDay = new Date(year, month, 1).getDay();
    const lastDay = new Date(year, month + 1, 0).getDate();
    const lastDayPrevMonth = new Date(year, month, 0).getDate();
    const calendarDaysDiv = document.getElementById('calendarDays');

    calendarDaysDiv.innerHTML = '';

    for (let i = firstDay - 1; i >= 0; i--) {
        calendarDaysDiv.appendChild(createDayElement(lastDayPrevMonth - i, month - 1, year, true));
    }

    for (let day = 1; day <= lastDay; day++) {
        calendarDaysDiv.appendChild(createDayElement(day, month, year, false));
    }

    const remainingCells = 42 - calendarDaysDiv.children.length;

    for (let day = 1; day <= remainingCells; day++) {
        calendarDaysDiv.appendChild(createDayElement(day, month + 1, year, true));
    }
}

function createDayElement(day, month, year, isOtherMonth) {
    const element = document.createElement('div');
    element.className = 'calendar-day';

    if (month < 0) {
        month = 11;
        year--;
    } else if (month > 11) {
        month = 0;
        year++;
    }

    element.innerHTML = `<span class="calendar-day-number">${day}</span>`;

    if (isOtherMonth) {
        element.classList.add('outro-mes');
        return element;
    }

    const dateKey = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
    const dayData = calendarData[dateKey];

    if (!dayData) {
        element.classList.add('sem-dados');
        element.title = 'Sem dados carregados para este dia';
        return element;
    }

    element.classList.add(isStandByDay(dayData) ? 'stand-by' : 'dia-normal');

    const highlight = getDayHighlight(dayData);

    if (highlight) {
        element.classList.add(`tipo-${highlight.className}`);
        element.insertAdjacentHTML('beforeend', `<span class="calendar-day-badge">${highlight.label}</span>`);
    }

    if (dayData.Descricao) {
        element.title = dayData.Tipo ? `${dayData.Descricao} - ${dayData.Tipo}` : dayData.Descricao;
    }

    return element;
}

function updateSummary() {
    const totals = Object.values(calendarData).reduce(
        (acc, dayData) => {
            if (isStandByDay(dayData)) {
                acc.off++;
            } else {
                acc.uteis++;
            }

            return acc;
        },
        { uteis: 0, off: 0 }
    );

    document.getElementById('totalNormal').textContent = totals.uteis;
    document.getElementById('totalStandBy').textContent = totals.off;
}

function previousMonth() {
    currentDate.setMonth(currentDate.getMonth() - 1);
    updateMonthInput();
    loadCalendar();
}

function nextMonth() {
    currentDate.setMonth(currentDate.getMonth() + 1);
    updateMonthInput();
    loadCalendar();
}

function goToToday() {
    currentDate = new Date();
    updateMonthInput();
    loadCalendar();
}

function updateMonthInput() {
    const year = currentDate.getFullYear();
    const month = String(currentDate.getMonth() + 1).padStart(2, '0');
    document.getElementById('monthYear').value = `${year}-${month}`;
}

function exportData() {
    let csv = 'Data,DiaSemana,DiaUtil,Feriado,PontoFacultativo,Descricao,Tipo\n';

    for (const date in calendarData) {
        const data = calendarData[date];
        csv += `${date},${data.DiaSemana},${data.DiaUtil},${data.Feriado},${data.PontoFacultativo},${data.Descricao || ''},${data.Tipo || ''}\n`;
    }

    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);

    link.setAttribute('href', url);
    link.setAttribute('download', `calendario_${new Date().toISOString().split('T')[0]}.csv`);
    link.style.visibility = 'hidden';

    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
}
