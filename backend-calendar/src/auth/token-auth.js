const crypto = require('crypto');

const DEFAULT_USERS = [
    {
        username: process.env.AUTH_DEFAULT_USER || 'admin',
        password: process.env.AUTH_DEFAULT_PASSWORD || 'admin123',
        roles: ['ADMIN']
    }
];

function base64UrlEncode(value) {
    return Buffer.from(JSON.stringify(value)).toString('base64url');
}

function base64UrlDecode(value) {
    return JSON.parse(Buffer.from(value, 'base64url').toString('utf8'));
}

function getSecret() {
    return process.env.AUTH_SECRET || 'people-transportation-dev-secret';
}

function signPayload(header, payload) {
    const unsignedToken = `${base64UrlEncode(header)}.${base64UrlEncode(payload)}`;
    const signature = crypto
        .createHmac('sha256', getSecret())
        .update(unsignedToken)
        .digest('base64url');

    return `${unsignedToken}.${signature}`;
}

function safeEqual(left, right) {
    const leftBuffer = Buffer.from(left);
    const rightBuffer = Buffer.from(right);

    return leftBuffer.length === rightBuffer.length && crypto.timingSafeEqual(leftBuffer, rightBuffer);
}

function parseUsers() {
    if (!process.env.AUTH_USERS_JSON) {
        return DEFAULT_USERS;
    }

    try {
        const users = JSON.parse(process.env.AUTH_USERS_JSON);
        return Array.isArray(users) && users.length > 0 ? users : DEFAULT_USERS;
    } catch (error) {
        console.error('AUTH_USERS_JSON invalido. Usando usuario padrao de desenvolvimento.', error.message);
        return DEFAULT_USERS;
    }
}

function createToken(user, expiresInSeconds = Number(process.env.AUTH_TOKEN_TTL_SECONDS || 3600)) {
    const now = Math.floor(Date.now() / 1000);
    const header = { alg: 'HS256', typ: 'JWT' };
    const payload = {
        sub: user.username,
        roles: user.roles || [],
        iat: now,
        exp: now + expiresInSeconds
    };

    return signPayload(header, payload);
}

function verifyToken(token) {
    if (!token || typeof token !== 'string') {
        throw new Error('Token ausente');
    }

    const parts = token.split('.');
    if (parts.length !== 3) {
        throw new Error('Token invalido');
    }

    const [encodedHeader, encodedPayload, signature] = parts;
    const expectedSignature = crypto
        .createHmac('sha256', getSecret())
        .update(`${encodedHeader}.${encodedPayload}`)
        .digest('base64url');

    if (!safeEqual(signature, expectedSignature)) {
        throw new Error('Assinatura do token invalida');
    }

    const payload = base64UrlDecode(encodedPayload);
    if (payload.exp && payload.exp < Math.floor(Date.now() / 1000)) {
        throw new Error('Token expirado');
    }

    return payload;
}

function authenticateCredentials(username, password) {
    const user = parseUsers().find((candidate) => candidate.username === username);

    if (!user || !safeEqual(String(user.password), String(password))) {
        return null;
    }

    return {
        username: user.username,
        roles: user.roles || []
    };
}

function getBearerToken(req) {
    const authorization = req.headers.authorization || '';
    const [scheme, token] = authorization.split(' ');

    return scheme === 'Bearer' ? token : null;
}

function authenticateRequest(req, res, next) {
    try {
        req.user = verifyToken(getBearerToken(req));
        next();
    } catch (error) {
        res.status(401).json({ error: error.message });
    }
}

function requireRole(role) {
    return (req, res, next) => {
        if (!req.user || !Array.isArray(req.user.roles) || !req.user.roles.includes(role)) {
            return res.status(403).json({ error: 'Permissao insuficiente' });
        }

        return next();
    };
}

module.exports = {
    authenticateCredentials,
    authenticateRequest,
    createToken,
    requireRole,
    verifyToken
};
