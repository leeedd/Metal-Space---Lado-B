package Jogo;

import javax.swing.SwingUtilities;

/** Inicializacao atual: tela 16 bits; menu historico opcional pela tecla M/menu. */
public final class Inicializador {
    private Inicializador() { }
    public static void main(String[] args) {
        SomJogo.iniciar();
        // Prepara texturas antes da simulacao: evita pausas na primeira luta.
        for (int id=1; id<=8; id++) for (int forma=1; forma<=2; forma++)
            Sprites16Bits.carregarAnimado(id, forma, 0);
        SwingUtilities.invokeLater(TelaInicial::abrir);
        preCarregarCombate();
    }

    /**
     * Artes do combate (tiros, inimigos, itens, poderes, HUD, base e mapa): carregadas enquanto a abertura toca.
     * Sem isso, cada uma carregava na primeira vez que aparecia e o jogo travava de 0,1 a 1,4 s no meio da luta.
     * Ficam de fora chefes e cenas, que pesam a maior parte da memoria e carregam nas transicoes.
     * Nenhuma dessas classes depende de outra ao inicializar (sem risco de travar duas threads).
     */
    static final String[] COMBATE = {"TiroChefe", "Arte094", "Arte095", "Arte098", "AtlasInimigos", "Piratas094", "HudEnergia", "HudMetal",
            "ArteNaves", "Item", "ItensArte", "Asteroide", "LaserConvergencia", "Poderes099", "RadioPirata", "Pecas", "InimigosUniversos",
            "LulaVisual", "SpritesExtras", "EstacaoAliada", "MapaEstelar", "Pausa099", "Nave"};

    static void preCarregarCombate() {
        Thread t = Thread.currentThread();
        int prioridade = t.getPriority();
        t.setPriority(Thread.MIN_PRIORITY); // a abertura (na thread da tela) tem preferencia
        try {
            for (String c : COMBATE) {
                try { Class.forName("Jogo." + c, true, Inicializador.class.getClassLoader()); }
                catch (Throwable e) { System.err.println("Pre-carga de " + c + " falhou: " + e); }
            }
        } finally { t.setPriority(prioridade); }
    }
}
