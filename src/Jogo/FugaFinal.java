package Jogo;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;

/**
 * Fuga do buraco negro (etapa 1, prototipo): depois do ultimo chefe o inferno desaba e a nave sobe pelo rio de lava ate a saida.
 * Camera em primeira pessoa, dentro da cabine. O jogador pilota (manche), controla a potencia (alavanca), atira, lanca misseis e bombas.
 * Partes: FugaFinal (estado, aceleracao, armas e danos), FugaPista (mundo pseudo-3D e obstaculos), FugaCockpit (cabine, maos e painel).
 *
 * Controles: setas = manche (A/D tambem viram); W/S = alavanca de potencia; ESPACO = canhoes; F = missil; B = bomba.
 */
final class FugaFinal {
    static final double DIST_TOTAL = 2600, INTRO = 3.0;
    // sistemas monitorados no diagnostico (0 a 100)
    static final int ESTRUTURA = 0, ESCUDOS = 1, MOTORES = 2, ARMAS = 3, NAVEGACAO = 4, RADAR = 5;
    static final String[] SISTEMAS = {"ESTRUTURA", "ESCUDOS", "MOTORES", "ARMAS", "NAVEGACAO", "RADAR"};
    final double[] sist = {100, 100, 100, 100, 100, 100};
    final int modelo; final FugaPista pista = new FugaPista(); final FugaCockpit cabine;
    // pilotagem
    double sx, sy = .1, potencia = .45, vel = 14, tempo, intro = INTRO;
    boolean cima, baixo, esq, dir, acelera, desacelera, atirando, empurrando; double manX, manY; // empurrando: manche todo a frente (comando final); manX/manY: inclinacao do manche (-1 a 1), para as maos
    // armas
    int municao = 900, misseis = 10, bombas = 3; double calor, recargaMissil, cadencia, gatilho, polegar, chaveBomba;
    // escudo, temperatura, dobra, colapso
    double escudo = 100, temperatura = 40, dobra, colapsoDist = -140, tremor, impacto, alarme, falha, travaArmas;
    String fala = ""; double falaT; private int checkpoint; private double recomeco;
    boolean terminou, semMaoDireita; FugaClimax climax;
    // sequencia de ignicao da dobra: comandos no tempo certo durante a subida; a primeira tentativa falha de proposito
    static final String[] PASSOS = {"REATOR", "BOBINAS", "ESTABILIZADOR", "IGNICAO"};
    int ignTentativa, ignPasso = -1, ignTecla; double ignT, ignJanela, ignAgulha, ignEspera = -1, ignProxima = .18; boolean ignOk; String ignMsg = ""; double ignMsgT;

    FugaFinal(int modelo) { this.modelo = modelo; cabine = new FugaCockpit(modelo); falar("INFERNO EM COLAPSO. SUBA PELO RIO DE LAVA ATE A SAIDA."); SomJogo.tocar(SomJogo.Efeito.ALERTA); }

    double progresso() { return Math.min(1, pista.distancia / DIST_TOTAL); }
    double potenciaMax() { return .45 + .55 * sist[MOTORES] / 100; } // motor ferido nao chega na potencia toda
    void falar(String s) { fala = s; falaT = 3.2; }

    void atualizar(double dt) {
        if (climax != null) { tempo += dt; falaT = Math.max(0, falaT - dt); climax.atualizar(dt); return; }
        tempo += dt; falaT = Math.max(0, falaT - dt); impacto = Math.max(0, impacto - dt * 2.5); gatilho = Math.max(0, gatilho - dt); polegar = Math.max(0, polegar - dt); chaveBomba = Math.max(0, chaveBomba - dt);
        if (intro > 0) { intro = Math.max(0, intro - dt); return; }
        if (recomeco > 0) { if ((recomeco -= dt) <= 0) reiniciar(); return; }
        // alavanca de potencia: o jogador acelera e desacelera quando quiser
        if (acelera) potencia = Math.min(potenciaMax(), potencia + dt * .45); if (desacelera) potencia = Math.max(0, potencia - dt * .55); potencia = Math.min(potencia, potenciaMax());
        double alvoVel = 6 + 42 * potencia; vel += (alvoVel - vel) * Math.min(1, dt * 1.4);
        // manche: quanto mais rapido, mais dura a manobra
        double ix = (esq ? -1 : 0) + (dir ? 1 : 0), iy = (cima ? -1 : 0) + (baixo ? 1 : 0); manX += (ix - manX) * Math.min(1, dt * 8); manY += (iy - manY) * Math.min(1, dt * 8);
        double resp = 1.9 - vel / 48 * .7; if (sist[NAVEGACAO] < 40) resp *= .75;
        sx = Math.max(-1.35, Math.min(1.35, sx + manX * resp * dt)); sy = Math.max(-.9, Math.min(.9, sy + manY * resp * .8 * dt));
        // temperatura: potencia alta e voo rente a lava esquentam o motor
        double alvoT = 35 + potencia * 45 + Math.max(0, sy - .4) * 90 + (100 - sist[MOTORES]) * .2; temperatura += (alvoT - temperatura) * Math.min(1, dt * .5);
        if (temperatura > 95) { ferir(MOTORES, dt * 6); alarme = Math.max(alarme, .3); }
        if (sy > .82) { ferir(ESTRUTURA, dt * 10); escudo = Math.max(0, escudo - dt * 20); impacto = Math.max(impacto, .2); } // raspando na lava
        // o inferno desaba atras: se o jogador anda devagar demais, o colapso alcanca
        colapsoDist += (17 + progresso() * 6) * dt; double atras = pista.distancia - colapsoDist;
        if (atras < 0) { ferir(ESTRUTURA, dt * 14); ferir(MOTORES, dt * 5); impacto = Math.max(impacto, .4); if ((int) (tempo * 2) != (int) ((tempo - dt) * 2)) falar("O COLAPSO ALCANCOU A NAVE! ACELERE!"); }
        // escudo recarrega pelo gerador (que tambem pode estar ferido)
        escudo = Math.min(100, escudo + dt * 6 * sist[ESCUDOS] / 100);
        // armas
        calor = Math.max(0, calor - dt * 35); recargaMissil = Math.max(0, recargaMissil - dt); cadencia -= dt; travaArmas = Math.max(0, travaArmas - dt);
        if (atirando && cadencia <= 0 && municao > 0 && calor < 100 && travaArmas <= 0) { cadencia = .075;
            if (sist[ARMAS] < 40 && Math.random() < .25) { travaArmas = .5; falar("FALHA NOS CANHOES!"); }
            else { municao--; calor += 6; gatilho = .08; for (int l = -1; l <= 1; l += 2) { FugaPista.Tiro t = new FugaPista.Tiro(); t.x = sx + l * .2; t.y = sy + .1; t.z = .5; t.vx = -l * .05; pista.tiros.add(t); } SomJogo.tocar(SomJogo.Efeito.TIRO); } }
        // a dobra carrega durante a subida (fica pronta perto da saida)
        dobra = ignOk ? Math.min(100, dobra + dt * 4) : Math.min(dobra, 95);
        if (!ignOk && pista.distancia > DIST_TOTAL * .985) pista.distancia = DIST_TOTAL * .985; // sem a ignicao feita, a saida nao chega
        int cp = (int) (progresso() * 4); if (cp > checkpoint) { checkpoint = cp; if (cp < 4) falar("SETOR " + cp + "/4 VENCIDO. A SAIDA ESTA MAIS PERTO."); }
        pista.atualizar(dt, vel, sx, sy, progresso(), this);
        tremor = .6 + vel / 48 * 3.5 + impacto * 14 + (atras < 0 ? 4 : 0); alarme = Math.max(0, alarme - dt);
        for (int i = 0; i < 6; i++) if (sist[i] < 25) alarme = Math.max(alarme, .2);
        if (sist[ESTRUTURA] <= 0) { recomeco = 2.5; falar("NAVE PERDIDA. VOLTANDO AO ULTIMO SETOR..."); SomJogo.tocar(SomJogo.Efeito.DERROTA); }
        ignicao(dt);
        if (progresso() >= 1 && ignOk) { climax = new FugaClimax(this); } // perto da saida: a gargula
    }
    private void reiniciar() { double d = checkpoint * DIST_TOTAL / 4; pista.objs.clear(); pista.tiros.clear(); pista.distancia = d; colapsoDist = d - 140; for (int i = 0; i < 6; i++) sist[i] = Math.max(sist[i], 70); sist[ESTRUTURA] = 100; escudo = 100; potencia = .45; vel = 14; sx = 0; sy = .1; }
    void objsLimpar() { pista.objs.clear(); pista.tiros.clear(); atirando = acelera = desacelera = esq = dir = cima = baixo = false; }
    /** Nao socou o botao a tempo: o casco rompeu; volta ao ultimo setor e refaz o fim da subida. */
    void falhouClimax() { climax = null; semMaoDireita = false; empurrando = false; checkpoint = Math.min(checkpoint, 3); reiniciar(); for (int i = 0; i < 6; i++) sist[i] = 100; alarme = 0; falar("NAVE PERDIDA. VOLTANDO AO ULTIMO SETOR..."); }

    // ---------------- ignicao da dobra ----------------
    /** A cada tentativa aparecem os 4 passos; cada um pede uma tecla (1 a 4) quando a agulha passa na faixa verde.
     *  A 1a tentativa falha de proposito no ultimo passo (sobrecarga). Errar faz recomecar a sequencia. */
    private void ignicao(double dt) {
        ignMsgT = Math.max(0, ignMsgT - dt);
        if (ignOk) return;
        if (ignEspera > 0) { if ((ignEspera -= dt) <= 0) { ignEspera = -1; iniciarSequencia(); } return; }
        if (ignPasso < 0) { if (progresso() >= ignProxima) iniciarSequencia(); return; }
        ignT += dt; ignAgulha = (ignT / ignJanela) % 2; if (ignAgulha > 1) ignAgulha = 2 - ignAgulha; // a agulha vai e volta
        if (ignT > ignJanela * 4) falhaPasso("TEMPO ESGOTADO");
    }
    private void iniciarSequencia() { ignTentativa++; ignPasso = 0; novoPasso(); falar(ignTentativa == 1 ? "INICIANDO SEQUENCIA DE DOBRA." : "REINICIANDO SEQUENCIA DE DOBRA!"); SomJogo.tocar(SomJogo.Efeito.ALERTA); }
    private void novoPasso() { ignT = 0; ignTecla = (int) (Math.random() * 4); ignJanela = Math.max(.55, 1.1 - progresso() * .5 - ignPasso * .08); }
    private double faixa() { return .5; } private double larg() { return Math.max(.09, .2 - ignPasso * .025 - progresso() * .05); }
    private void comando(int k) {
        chaveBomba = .3; // a mao esquerda aciona o comando
        if (k != ignTecla || Math.abs(ignAgulha - faixa()) > larg() / 2) { falhaPasso(k != ignTecla ? "COMANDO ERRADO" : "FORA DO TEMPO"); return; }
        if (ignTentativa == 1 && ignPasso == 3) { falhaPasso("SOBRECARGA NO REATOR"); impacto = .8; ferir(MOTORES, 15); SomJogo.tocar(SomJogo.Efeito.EXPLOSAO); return; } // falha de proposito
        SomJogo.tocar(SomJogo.Efeito.CONFIRMAR); ignMsg = PASSOS[ignPasso] + " OK"; ignMsgT = .8; ignPasso++;
        if (ignPasso == 4) { ignOk = true; ignPasso = -1; falar("DOBRA ARMADA! CARREGANDO... CHEGUE A SAIDA!"); SomJogo.tocar(SomJogo.Efeito.EVOLUCAO); } else novoPasso();
    }
    private void falhaPasso(String m) { SomJogo.tocar(SomJogo.Efeito.ERRO); falar(m + "! SEQUENCIA ABORTADA."); ignMsg = "ABORTADA"; ignMsgT = 1.5; ignPasso = -1; ignEspera = 2.5; dobra = Math.max(0, dobra - 15); alarme = .8; }
    private void desenharIgnicao(Graphics2D g) {
        if (ignPasso < 0 && ignMsgT <= 0) return; int x = 300, y = 300, w = 360, h = 56;
        g.setColor(new Color(0, 0, 0, 190)); g.fillRect(x, y, w, h); g.setColor(new Color(200, 130, 255)); g.drawRect(x, y, w, h); g.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.BOLD, 13));
        if (ignPasso < 0) { g.setColor(ignMsg.equals("ABORTADA") ? new Color(255, 70, 50) : new Color(140, 255, 150)); g.drawString("IGNICAO DA DOBRA: " + ignMsg, x + 12, y + 32); return; }
        for (int i = 0; i < 4; i++) { g.setColor(i < ignPasso ? new Color(120, 255, 120) : i == ignPasso ? new Color(255, 230, 80) : new Color(90, 90, 110)); g.fillRect(x + 12 + i * 86, y + 8, 78, 4); }
        g.setColor(Color.WHITE); g.drawString(PASSOS[ignPasso] + "  -> TECLA " + (ignTecla + 1), x + 12, y + 27);
        int bx = x + 12, bw = w - 24, by = y + 36; g.setColor(new Color(40, 30, 50)); g.fillRect(bx, by, bw, 12);
        g.setColor(new Color(80, 220, 90)); int zw = (int) (bw * larg()); g.fillRect(bx + (int) (bw * faixa()) - zw / 2, by, zw, 12);
        g.setColor(Color.WHITE); int ax = bx + (int) (bw * ignAgulha); g.fillRect(ax - 2, by - 3, 4, 18);
    }
    void ferir(int s, double d) { sist[s] = Math.max(0, sist[s] - d); }
    /** Um obstaculo atingiu a nave: o escudo segura o que puder; o resto racha a estrutura e fere um sistema. */
    void colidiu(FugaPista.Obj o) {
        double dano = o.tipo == FugaPista.DESMORONE ? 34 : o.tipo == FugaPista.GARGULA ? 22 : o.tipo == FugaPista.COLUNA ? 18 : o.tipo == FugaPista.ROCHA ? 20 : 12;
        dano *= .7 + vel / 48 * .6; // mais rapido, batida mais forte
        double abs = Math.min(escudo, dano * 2) / 2; escudo -= abs * 2; dano -= abs;
        if (dano > 0) { ferir(ESTRUTURA, dano); int s = 1 + (int) (Math.random() * 5); ferir(s, dano * 1.3); if (sist[s] < 40) falar("DANO EM " + SISTEMAS[s] + "!"); }
        impacto = 1; alarme = .8; SomJogo.tocar(dano > 0 ? SomJogo.Efeito.DANO : SomJogo.Efeito.ESCUDO);
    }
    void abateu(FugaPista.Obj o) { SomJogo.tocar(SomJogo.Efeito.EXPLOSAO); }

    void tecla(int k, boolean p) {
        if (climax != null) { if (k == KeyEvent.VK_SPACE && p) climax.ativar(); return; }
        if (p && k >= KeyEvent.VK_1 && k <= KeyEvent.VK_4 && ignPasso >= 0) { comando(k - KeyEvent.VK_1); return; }
        if (k == KeyEvent.VK_LEFT || k == KeyEvent.VK_A) esq = p; else if (k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_D) dir = p;
        else if (k == KeyEvent.VK_UP) cima = p; else if (k == KeyEvent.VK_DOWN) baixo = p;
        else if (k == KeyEvent.VK_W) acelera = p; else if (k == KeyEvent.VK_S) desacelera = p;
        else if (k == KeyEvent.VK_SPACE) atirando = p;
        else if (k == KeyEvent.VK_F && p) missil(); else if (k == KeyEvent.VK_B && p) bomba();
    }
    void missil() { if (intro > 0 || misseis <= 0 || recargaMissil > 0) return; polegar = .25; FugaPista.Obj a = pista.alvo(sx, sy); FugaPista.Tiro t = new FugaPista.Tiro(); t.missil = true; t.x = sx; t.y = sy + .15; t.z = .6; t.alvo = a; pista.tiros.add(t); misseis--; recargaMissil = 1.0; SomJogo.tocar(SomJogo.Efeito.LASER); }
    void bomba() { if (intro > 0 || bombas <= 0) return; chaveBomba = .5; bombas--; pista.bomba(); impacto = .6; SomJogo.tocar(SomJogo.Efeito.NUCLEAR); falar("BOMBA LANCADA."); }

    void desenhar(Graphics2D g, Nave nave) {
        if (climax != null) { climax.desenhar(g, nave, cabine); if (falaT > 0) cabine.texto(g, fala, 70, new Color(140, 255, 150)); return; }
        if (intro > 0) { cabine.desenharIntro(g, this, nave); return; }
        Graphics2D v = (Graphics2D) g.create(); double tr = tremor; v.translate((Math.random() - .5) * tr, (Math.random() - .5) * tr);
        pista.desenhar(v, sx, sy, tempo, Math.max(0, Math.min(1, (colapsoDist - pista.distancia + 40) / 40)));
        cabine.desenhar(v, this); v.dispose(); desenharIgnicao(g);
        if (recomeco > 0) { g.setColor(new Color(0, 0, 0, (int) (255 * Math.min(1, (2.5 - recomeco) / 1.2)))); g.fillRect(0, 0, 960, 600); cabine.texto(g, fala, 300, new Color(255, 80, 60)); }
    }
}
