// Sistema de Confirmação de Presença - Versão Local
// Armazena dados em localStorage (sem banco de dados)

let students = [];

// Inicializa ao carregar a página
document.addEventListener('DOMContentLoaded', function() {
    loadStudentsFromStorage();
    updateStatistics();
});

// Carrega alunos do localStorage
function loadStudentsFromStorage() {
    const stored = localStorage.getItem('students');
    if (stored) {
        students = JSON.parse(stored);
    }
    updateTable();
}

// Salva alunos no localStorage
function saveStudentsToStorage() {
    localStorage.setItem('students', JSON.stringify(students));
    updateStatistics();
}

// Adiciona novo aluno
function addStudent(event) {
    event.preventDefault();
    
    const name = document.getElementById('studentName').value.trim();
    const email = document.getElementById('studentEmail').value.trim();
    
    if (!name || !email) {
        alert('Por favor, preencha nome e email!');
        return;
    }
    
    // Verifica se email já existe
    if (students.some(s => s.email === email)) {
        alert('Este email já está cadastrado!');
        return;
    }
    
    // Cria novo aluno
    const newStudent = {
        id: Date.now(),
        nome: name,
        email: email,
        status: 'PENDENTE',
        dataCriacao: new Date().toLocaleDateString('pt-BR')
    };
    
    students.push(newStudent);
    saveStudentsToStorage();
    
    // Limpa o formulário
    document.getElementById('addStudentForm').reset();
    updateTable();
}

// Atualiza a tabela de alunos
function updateTable() {
    const tbody = document.getElementById('studentsTableBody');
    
    if (students.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" class="loading">Nenhum aluno adicionado ainda</td></tr>';
        return;
    }
    
    tbody.innerHTML = students.map(student => `
        <tr>
            <td>${student.nome}</td>
            <td>${student.email}</td>
            <td>
                <span class="status-badge ${student.status.toLowerCase()}">
                    ${getStatusText(student.status)}
                </span>
            </td>
            <td>
                <button class="btn-table btn-present" onclick="setPresent(${student.id})">✓ Presente</button>
                <button class="btn-table btn-absent" onclick="setAbsent(${student.id})">✗ Ausente</button>
                <button class="btn-table btn-delete" onclick="deleteStudent(${student.id})">🗑️ Remover</button>
            </td>
        </tr>
    `).join('');
}

// Define aluno como presente
function setPresent(studentId) {
    const student = students.find(s => s.id === studentId);
    if (student) {
        student.status = 'PRESENTE';
        saveStudentsToStorage();
        updateTable();
    }
}

// Define aluno como ausente
function setAbsent(studentId) {
    const student = students.find(s => s.id === studentId);
    if (student) {
        student.status = 'AUSENTE';
        saveStudentsToStorage();
        updateTable();
    }
}

// Deleta um aluno
function deleteStudent(studentId) {
    if (confirm('Tem certeza que deseja remover este aluno?')) {
        students = students.filter(s => s.id !== studentId);
        saveStudentsToStorage();
        updateTable();
    }
}

// Atualiza as estatísticas
function updateStatistics() {
    const total = students.length;
    const confirmed = students.filter(s => s.status !== 'PENDENTE').length;
    const notConfirmed = total - confirmed;
    
    document.getElementById('totalStudents').textContent = total;
    document.getElementById('confirmedCount').textContent = confirmed;
    document.getElementById('notConfirmedCount').textContent = notConfirmed;
}

// Retorna o texto do status em português
function getStatusText(status) {
    const statusMap = {
        'PRESENTE': 'Presente',
        'AUSENTE': 'Ausente',
        'PENDENTE': 'Pendente'
    };
    return statusMap[status] || status;
}

// Limpa todos os dados
function clearAll() {
    if (confirm('Tem certeza que deseja limpar TODOS os alunos? Esta ação não pode ser desfeita!')) {
        students = [];
        localStorage.clear();
        updateTable();
        updateStatistics();
    }
}

