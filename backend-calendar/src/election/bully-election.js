class BullyElection {
    constructor() {
        this.nodeId = Number(process.env.ELECTION_NODE_ID || process.env.NODE_ID || 1);
        this.nodes = parseNodes(this.nodeId);
        this.leaderId = Number(process.env.ELECTION_INITIAL_LEADER_ID || 0);
        this.electionInProgress = false;
        this.lastElectionAt = null;
    }

    status() {
        return {
            nodeId: this.nodeId,
            leaderId: this.leaderId,
            electionInProgress: this.electionInProgress,
            lastElectionAt: this.lastElectionAt,
            nodes: this.nodes
        };
    }

    async startElection() {
        if (this.electionInProgress) {
            return this.status();
        }

        this.electionInProgress = true;
        this.lastElectionAt = new Date().toISOString();

        const higherNodes = this.nodes.filter((node) => node.id > this.nodeId);
        let higherNodeResponded = false;

        for (const node of higherNodes) {
            try {
                const response = await postJson(`${node.address}/api/election/election`, { nodeId: this.nodeId });
                higherNodeResponded = higherNodeResponded || response.received === true;
            } catch {
                // Node indisponivel: o algoritmo Bully simplesmente segue tentando os proximos maiores.
            }
        }

        if (!higherNodeResponded) {
            await this.becomeLeader();
        }

        this.electionInProgress = false;
        return this.status();
    }

    async receiveElection(senderNodeId) {
        if (senderNodeId < this.nodeId) {
            setTimeout(() => {
                this.startElection().catch((error) => console.error('Erro ao iniciar eleicao:', error));
            }, 0);
        }

        return { received: true, nodeId: this.nodeId };
    }

    async becomeLeader() {
        this.leaderId = this.nodeId;

        const otherNodes = this.nodes.filter((node) => node.id !== this.nodeId);
        await Promise.allSettled(
            otherNodes.map((node) => postJson(`${node.address}/api/election/leader`, { leaderId: this.nodeId }))
        );
    }

    setLeader(leaderId) {
        const numericLeaderId = Number(leaderId);
        if (!Number.isInteger(numericLeaderId) || numericLeaderId <= 0) {
            throw new Error('leaderId invalido');
        }

        if (numericLeaderId >= this.leaderId) {
            this.leaderId = numericLeaderId;
        }

        return this.status();
    }

    async discoverLeader() {
        if (this.leaderId) {
            return this.status();
        }

        for (const node of this.nodes.filter((candidate) => candidate.id !== this.nodeId)) {
            try {
                const response = await getJson(`${node.address}/api/election/leader`);
                if (response.leaderId) {
                    this.leaderId = Number(response.leaderId);
                    return this.status();
                }
            } catch {
                // Continua procurando nos demais nos.
            }
        }

        return this.startElection();
    }

    async monitorLeader() {
        if (!this.leaderId || this.leaderId === this.nodeId) {
            return this.status();
        }

        const leader = this.nodes.find((node) => node.id === this.leaderId);
        if (!leader) {
            this.leaderId = 0;
            return this.startElection();
        }

        try {
            await getJson(`${leader.address}/api/election/ping`);
        } catch {
            this.leaderId = 0;
            await this.startElection();
        }

        return this.status();
    }
}

function parseNodes(nodeId) {
    if (process.env.ELECTION_NODES_JSON) {
        try {
            const nodes = JSON.parse(process.env.ELECTION_NODES_JSON);
            if (Array.isArray(nodes)) {
                return nodes.map(normalizeNode).filter(Boolean);
            }
        } catch (error) {
            console.error('ELECTION_NODES_JSON invalido:', error.message);
        }
    }

    if (process.env.ELECTION_NODE_URLS) {
        return process.env.ELECTION_NODE_URLS
            .split(',')
            .map((entry) => {
                const [id, address] = entry.split('=');
                return normalizeNode({ id: Number(id), address });
            })
            .filter(Boolean);
    }

    return [{ id: nodeId, address: process.env.PUBLIC_BASE_URL || `http://localhost:${process.env.PORT || 3001}` }];
}

function normalizeNode(node) {
    const id = Number(node.id);
    const address = String(node.address || '').replace(/\/$/, '');

    if (!Number.isInteger(id) || id <= 0 || !address) {
        return null;
    }

    return { id, address };
}

async function requestJson(url, options = {}) {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), Number(process.env.ELECTION_REQUEST_TIMEOUT_MS || 1500));

    try {
        const response = await fetch(url, {
            ...options,
            signal: controller.signal,
            headers: {
                'Content-Type': 'application/json',
                ...(options.headers || {})
            }
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        return response.json();
    } finally {
        clearTimeout(timeout);
    }
}

function getJson(url) {
    return requestJson(url);
}

function postJson(url, body) {
    return requestJson(url, {
        method: 'POST',
        body: JSON.stringify(body)
    });
}

module.exports = {
    BullyElection
};
