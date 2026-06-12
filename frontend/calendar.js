// Gerenciador de Calendário Acadêmico

const API_URL = 'http://localhost:3001/api';
let currentDate = new Date();
let selectedFaculdade = 'FURB';
let calendarData = {};
let useDatabase = true;

// Inicialização
document.addEventListener('DOMContentLoaded', function() {
    initializeCalendar();
});

// Inicializa o calendário
async function initializeCalendar() {
    // Verifica se pode conectar ao banco
    const isConnected = await checkDatabaseConnection();
    useDatabase = isConnected;

    if (useDatabase) {
        console.log('✓ Usando PostgreSQL');
    } else {
        console.log('⚠️ PostgreSQL não disponível. Usando localStorage.');
        loadCalendarFromStorage();
    }

    // Seleciona "Todas" por padrão
    document.getElementById('faculdadeSelect').value = 'all';
    selectedFaculdade = 'all';

    setTodayDate();
    await loadCalendar();
}

// Verifica conexão com banco de dados
async function checkDatabaseConnection() {
    try {
        const response = await fetch(`${API_URL}/health`);
        return response.ok;
    } catch (error) {
        return false;
    }
}

// Define o input de mês para hoje
function setTodayDate() {
    const today = new Date();
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, '0');
    document.getElementById('monthYear').value = `${year}-${month}`;
}

// Carrega dados do localStorage
function loadCalendarFromStorage() {
    const stored = localStorage.getItem('calendar');
    if (stored) {
        calendarData = JSON.parse(stored);
    }
}

// Salva dados no localStorage
function saveCalendarToStorage() {
    localStorage.setItem('calendar', JSON.stringify(calendarData));
}

// Carrega/renderiza o calendário
async function loadCalendar() {
    const monthYearInput = document.getElementById('monthYear').value;
    if (monthYearInput) {
        const [year, month] = monthYearInput.split('-');
        currentDate = new Date(year, parseInt(month) - 1, 1);
    }
    selectedFaculdade = document.getElementById('faculdadeSelect').value;

    if (useDatabase) {
        await loadCalendarFromDatabase();
    } else {
        loadCalendarFromStorage();
    }

    renderCalendar();
    updateSummary();
}

// Carrega do banco de dados PostgreSQL
async function loadCalendarFromDatabase() {
    try {
        const year = currentDate.getFullYear();
        const month = String(currentDate.getMonth() + 1).padStart(2, '0');
        const mes = `${year}-${month}`;

        const url = `${API_URL}/calendar?mes=${mes}`;
        const response = await fetch(url);

        if (!response.ok) {
            console.error('Erro ao carregar calendário:', response.status);
            return;
        }

        const data = await response.json();
        calendarData = {};

           // Estrutura: calendarData[data] = { DiaUtil, FURB, UFSC, SENAI, ... }
           data.forEach(item => {
               // Extrai apenas a data (YYYY-MM-DD) se o item.Data estiver em formato ISO
               let dateKey = item.Data;
               if (dateKey.includes('T')) {
                   dateKey = dateKey.split('T')[0];
               }
           
               calendarData[dateKey] = {
                DiaUtil: item.DiaUtil,
                DiaSemana: item.DiaSemana,
                DataFormatada: item.DataFormatada,
                FURB: item.FURB,
                UFSC: item.UFSC,
                SENAI: item.SENAI,
                Unisosciesc: item.Unisosciesc,
                Uniasselvi: item.Uniasselvi
            };
        });
    } catch (error) {
        console.error('Erro ao carregar do banco:', error);
    }
}

// Renderiza o calendário no HTML
function renderCalendar() {
    const year = currentDate.getFullYear();
    const month = currentDate.getMonth();

    // Atualiza o header
    const monthNames = [
        'Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho',
        'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'
    ];
    document.getElementById('currentMonth').textContent = `${monthNames[month]} de ${year}`;

    // Pega o primeiro dia do mês
    const firstDay = new Date(year, month, 1).getDay();
    const lastDay = new Date(year, month + 1, 0).getDate();
    const lastDayPrevMonth = new Date(year, month, 0).getDate();

    const calendarDaysDiv = document.getElementById('calendarDays');
    calendarDaysDiv.innerHTML = '';

    // Dias do mês anterior (cinzento)
    for (let i = firstDay - 1; i >= 0; i--) {
        const day = lastDayPrevMonth - i;
        const dayElement = createDayElement(day, month - 1, year, true);
        calendarDaysDiv.appendChild(dayElement);
    }

    // Dias do mês atual
    for (let day = 1; day <= lastDay; day++) {
        const dayElement = createDayElement(day, month, year, false);
        calendarDaysDiv.appendChild(dayElement);
    }

    // Dias do próximo mês (cinzento)
    const totalCells = calendarDaysDiv.children.length;
    const remainingCells = 42 - totalCells;
    for (let day = 1; day <= remainingCells; day++) {
        const dayElement = createDayElement(day, month + 1, year, true);
        calendarDaysDiv.appendChild(dayElement);
    }
}

// Cria um elemento de dia
function createDayElement(day, month, year, isOtherMonth) {
    const element = document.createElement('div');
    element.className = 'calendar-day';

    // Ajusta o mês se necessário
    if (month < 0) {
        month = 11;
        year--;
    } else if (month > 11) {
        month = 0;
        year++;
    }

    if (isOtherMonth) {
        element.classList.add('outro-mes');
        element.innerHTML = `<span class="calendar-day-number">${day}</span>`;
        return element;
    }

    // Formato da data YYYY-MM-DD
    const dateKey = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;

    // Pega dados do dia
    const dayData = calendarData[dateKey];
    
    if (!dayData) {
        element.classList.add('sem-dados');
        element.innerHTML = `<span class="calendar-day-number">${day}</span>`;
        return element;
    }

    let htmlContent = `<span class="calendar-day-number">${day}</span>`;

    if (selectedFaculdade === 'all') {
        // Modo "Todas as Faculdades"
        const dayOfWeek = dayData.DiaSemana; // 0 = domingo, 6 = sábado
        const isWeekend = dayOfWeek === 0 || dayOfWeek === 6;
        
        if (isWeekend) {
            element.classList.add('stand-by');
        } else {
            element.classList.add('dia-normal');
        }
    } else {
        // Modo faculdade específica
        const dayOfWeek = dayData.DiaSemana;
        const isWeekend = dayOfWeek === 0 || dayOfWeek === 6;
        const hasFeriado = dayData[selectedFaculdade] === false;
        const isStandBy = isWeekend || hasFeriado;
        
        if (isStandBy) {
            element.classList.add('stand-by');
        } else {
            element.classList.add('dia-normal');
        }
    }

    element.innerHTML = htmlContent;
    element.onclick = () => toggleDay(dateKey, element, dayData);

    return element;
}

// Marca/desmarca um dia como feriado ou normal para a faculdade
async function toggleDay(dateKey, element, dayData) {
    const dayOfWeek = dayData.DiaSemana;
    const isWeekend = dayOfWeek === 0 || dayOfWeek === 6;

    if (isWeekend) {
        showFeedback('Não é possível alterar fins de semana');
        return;
    }

    let newStatus;

    if (selectedFaculdade === 'all') {
        showFeedback('Selecione uma faculdade específica para marcar feriados');
        return;
    } else {
        // Para faculdade específica
        const isFaculdadeComFeriado = dayData[selectedFaculdade] === false;
        newStatus = !isFaculdadeComFeriado; // true = normal, false = feriado
        dayData[selectedFaculdade] = newStatus;

        if (useDatabase) {
            await saveDayToDatabase(dateKey, newStatus);
        }
    }

    // Re-renderiza apenas esse dia e atualiza resumo
    renderSingleDay(dateKey, element);
    updateSummary();
}

// Helper para feedback visual
function showFeedback(message) {
    const feedback = document.getElementById('feedbackMessage');
    if (!feedback) {
        alert(message);
    } else {
        feedback.textContent = message;
        setTimeout(() => {
            feedback.textContent = '';
        }, 3000);
    }}

// Re-renderiza um único dia
function renderSingleDay(dateKey, element) {
    const [year, month, day] = dateKey.split('-');
    const dayData = calendarData[dateKey];
    
    if (!dayData) return;

    element.className = 'calendar-day';
    let htmlContent = `<span class="calendar-day-number">${parseInt(day)}</span>`;

    const dayOfWeek = dayData.DiaSemana;
    const isWeekend = dayOfWeek === 0 || dayOfWeek === 6;
    
    if (selectedFaculdade === 'all') {
        // Modo "Todas as Faculdades"
        if (isWeekend) {
            element.classList.add('stand-by');
        } else {
            element.classList.add('dia-normal');
        }
    } else {
        // Modo faculdade específica
        const hasFeriado = dayData[selectedFaculdade] === false;
        const isStandBy = isWeekend || hasFeriado;
        
        if (isStandBy) {
            element.classList.add('stand-by');
        } else {
            element.classList.add('dia-normal');
        }
    }

    element.innerHTML = htmlContent;
}

// Salva um dia no banco de dados
async function saveDayToDatabase(dateKey, diaUtil) {
    try {
        const response = await fetch(`${API_URL}/calendar`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                data: dateKey,
                faculdade: selectedFaculdade,
                diaUtil: diaUtil
            })
        });

        if (!response.ok) {
            throw new Error('Erro ao salvar');
        }
    } catch (error) {
        console.error('Erro ao salvar dia:', error);
        alert('❌ Erro ao salvar dia');
    }
}

// Atualiza o resumo
function updateSummary() {
    const year = currentDate.getFullYear();
    const month = currentDate.getMonth();
    const lastDay = new Date(year, month + 1, 0).getDate();

    let totalNormal = 0;
    let totalStandBy = 0;

    for (let day = 1; day <= lastDay; day++) {
        const dateKey = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
        const dayData = calendarData[dateKey];

        if (!dayData) continue;

        const dayOfWeek = dayData.DiaSemana;
        const isWeekend = dayOfWeek === 0 || dayOfWeek === 6;
        
        let isStandBy;
        
        if (selectedFaculdade === 'all') {
            // Modo "Todas as Faculdades" - contar baseado em fim de semana
            isStandBy = isWeekend;
        } else {
            // Para faculdade específica - considerar feriado ou fim de semana
            const hasFeriado = dayData[selectedFaculdade] === false;
            isStandBy = isWeekend || hasFeriado;
        }

        if (isStandBy) {
            totalStandBy++;
        } else {
            totalNormal++;
        }
    }

    document.getElementById('totalNormal').textContent = totalNormal;
    document.getElementById('totalStandBy').textContent = totalStandBy;
}

// Navega para o mês anterior
function previousMonth() {
    currentDate.setMonth(currentDate.getMonth() - 1);
    updateMonthInput();
    loadCalendar();
}

// Navega para o próximo mês
function nextMonth() {
    currentDate.setMonth(currentDate.getMonth() + 1);
    updateMonthInput();
    loadCalendar();
}

// Vai para hoje
function goToToday() {
    currentDate = new Date();
    updateMonthInput();
    loadCalendar();
}

// Atualiza o input de mês
function updateMonthInput() {
    const year = currentDate.getFullYear();
    const month = String(currentDate.getMonth() + 1).padStart(2, '0');
    document.getElementById('monthYear').value = `${year}-${month}`;
}

// Salva o calendário
async function saveCalendar() {
    if (useDatabase) {
        alert('✓ Dados já foram salvos no banco!');
    } else {
        saveCalendarToStorage();
        alert('✓ Calendário salvo no navegador!');
    }
}

// Reseta para padrão
async function resetCalendar() {
    const faculdadeLabel = selectedFaculdade === 'all' ? 'todas as faculdades' : selectedFaculdade;
    if (confirm(`Tem certeza que deseja resetar ${faculdadeLabel} para o padrão do mês?`)) {
        const year = currentDate.getFullYear();
        const month = currentDate.getMonth();
        const lastDay = new Date(year, month + 1, 0).getDate();

        // Volta todos os dias para o padrão
        for (let day = 1; day <= lastDay; day++) {
            const dateKey = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
            const dayData = calendarData[dateKey];

            if (dayData) {
                const isDiaUtilPadrao = dayData.DiaUtil === true;

                if (selectedFaculdade === 'all') {
                    // Não faz reset no modo "Todas" pois é apenas visualização
                    continue;
                } else {
                    // Reseta apenas uma faculdade
                    dayData[selectedFaculdade] = isDiaUtilPadrao;

                    if (useDatabase) {
                        await saveDayToDatabase(dateKey, isDiaUtilPadrao);
                    }
                }
            }
        }

        renderCalendar();
        updateSummary();
        alert('✓ Seleção resetada para o padrão!');
    }
}

// Exporta os dados
function exportData() {
    let csv = 'Data,DiaSemana,DiaUtilPadrao,FURB,UFSC,SENAI,Unisosciesc,Uniasselvi\n';

    for (const date in calendarData) {
        const data = calendarData[date];
        csv += `${date},${data.DiaSemana},${data.DiaUtil},${data.FURB},${data.UFSC},${data.SENAI},${data.Unisosciesc},${data.Uniasselvi}\n`;
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

    alert('✓ Dados exportados com sucesso!');
}
