/*
 * Decompiled with CFR 0.152.
 */
package Jogo;








import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public final class TelaInicial
extends JPanel {
    private static final long serialVersionUID = 1L;
    private final JFrame janela;
    private final transient Image arteInicioNeutro = Assets.imagem("ui_inicio_neutro.png");
    /** Estrelas, asteroides e exaustao dos motores passando por tras da arte (feito na primeira vez que a tela aparece). */
    private transient FundoInicioAnimado animado;
    private final transient Image arteHangar = Assets.imagem("ui_hangar_aprovado.png");
    private final transient Image arteTrofeus = Assets.imagem("ui_colecao_aprovada.png");
    private static final String[] OPCOES = new String[]{"INICIAR", "HANGAR", "TROFEUS", "OPCOES", "RANKING", "SAIR"};
    private static final Rectangle[] AREAS_MENU = new Rectangle[]{new Rectangle(621, 284, 307, 47), new Rectangle(622, 334, 306, 46), new Rectangle(622, 382, 306, 46), new Rectangle(622, 430, 306, 46), new Rectangle(622, 479, 306, 46), new Rectangle(622, 526, 306, 46)};
    private static final Rectangle[] AREAS_HANGAR = new Rectangle[]{new Rectangle(18, 126, 180, 386), new Rectangle(204, 126, 180, 386), new Rectangle(390, 126, 180, 386), new Rectangle(576, 126, 180, 386), new Rectangle(762, 126, 180, 386)};
    private static final String[] REQUISITOS = new String[]{"NAVE INICIAL", "1 INSIGNIA", "3 INSIGNIAS", "5 INSIGNIAS", "ZERAR A CAMPANHA"};
    /** Fundo dos botoes do menu na arte (para cobrir o rotulo antigo). */
    private static final Color COR_BOTAO = new Color(10, 22, 38);
    private static final String[] NOMES_NAVES = new String[]{"RAPTOR MK-I", "SHADOW", "VALKYRIE", "BERSERKER", "METAL GOD"};
    private Pagina pagina = Pagina.INICIO;
    private final Timer animador;
    private double tempoHangar;
    private final java.util.Set<Integer> teclasAudio=new java.util.HashSet<Integer>();
    private int opcao;
    private boolean verPlacar;
    private final Placar placar = new Placar();
    private int modeloSelecionado;
    private int trofeuSelecionado = 8;

    private TelaInicial(JFrame jFrame) {SomJogo.pausar(false);SomJogo.iniciar();
        this.janela = jFrame;
        this.setPreferredSize(new Dimension(960, 600));
        this.setFocusable(true);
        this.setBackground(new Color(2, 7, 17));
        this.addKeyListener(new KeyAdapter(){

            @Override
            public void keyReleased(KeyEvent e){teclasAudio.remove(e.getKeyCode());}
            @Override public void keyPressed(KeyEvent keyEvent) {
                int n = keyEvent.getKeyCode();if((n==KeyEvent.VK_F7||n==KeyEvent.VK_F9||n==KeyEvent.VK_F10||n==KeyEvent.VK_F11||n==KeyEvent.VK_F12)&&!teclasAudio.add(n))return;if(PainelSom.tecla(n)){repaint();return;}if(n>=37&&n<=40&&pagina!=Pagina.INICIO)SomJogo.tocar(SomJogo.Efeito.MENU); // no menu inicial quem soa e a guitarra
                if(verPlacar){if(n==27||n==KeyEvent.VK_H)verPlacar=false;repaint();return;}
                if(n==KeyEvent.VK_H){verPlacar=true;repaint();return;}
                if (n == 27) {
                    TelaInicial.this.pagina = Pagina.INICIO;
                    TelaInicial.this.repaint();
                    return;
                }
                if (TelaInicial.this.pagina == Pagina.INICIO) {
                    if (n == 38) {
                        TelaInicial.this.opcao = Math.floorMod(TelaInicial.this.opcao - 1, OPCOES.length);
                        GuitarraMenu.passar(TelaInicial.this.opcao);
                    } else if (n == 40) {
                        TelaInicial.this.opcao = (TelaInicial.this.opcao + 1) % OPCOES.length;
                        GuitarraMenu.passar(TelaInicial.this.opcao);
                    } else if (n == 10 || n == 32) {
                        TelaInicial.this.acionarMenu(TelaInicial.this.opcao);
                    }
                } else if (TelaInicial.this.pagina == Pagina.HANGAR) {
                    if (n == 37) {
                        TelaInicial.this.modeloSelecionado = Math.floorMod(TelaInicial.this.modeloSelecionado - 1, 5);
                    } else if (n == 39) {
                        TelaInicial.this.modeloSelecionado = (TelaInicial.this.modeloSelecionado + 1) % 5;
                    } else if (n == 10 || n == 32) {
                        TelaInicial.this.iniciarPartida();
                    }
                } else if (TelaInicial.this.pagina == Pagina.TROFEUS) {
                    int[] nArray = Medalhas.ordem();
                    int n2 = 0;
                    for (int i = 0; i < nArray.length; ++i) {
                        if (nArray[i] != TelaInicial.this.trofeuSelecionado) continue;
                        n2 = i;
                    }
                    if (n == 37) {
                        n2 = Math.floorMod(n2 - 1, 8);
                    } else if (n == 39) {
                        n2 = (n2 + 1) % 8;
                    } else if (n == 38) {
                        n2 = Math.floorMod(n2 - 4, 8);
                    } else if (n == 40) {
                        n2 = (n2 + 4) % 8;
                    }
                    TelaInicial.this.trofeuSelecionado = nArray[n2];
                }
                TelaInicial.this.repaint();
            }
        });
        this.animador = new Timer(33, actionEvent -> {
            this.tempoHangar += 0.033;
            if (this.animado != null && this.pagina == Pagina.INICIO) this.animado.atualizar(0.033);
            if (this.pagina == Pagina.HANGAR || this.pagina == Pagina.INICIO) {
                this.repaint();
            }
        });
        this.animador.start();
        this.addMouseMotionListener(new MouseMotionAdapter(){

            @Override
            public void mouseMoved(MouseEvent mouseEvent) {if(PainelSom.aberto())return;
                if (TelaInicial.this.pagina != Pagina.INICIO) {
                    return;
                }
                for (int i = 0; i < AREAS_MENU.length; ++i) {
                    if (!AREAS_MENU[i].contains(mouseEvent.getPoint())) continue;
                    if(TelaInicial.this.opcao!=i)GuitarraMenu.passar(i);TelaInicial.this.opcao = i;
                    TelaInicial.this.repaint();
                    break;
                }
            }
        });
        this.addMouseListener(new MouseAdapter(){

            @Override
            public void mouseClicked(MouseEvent mouseEvent) {if(PainelSom.aberto())return;
                int n = mouseEvent.getX();
                int n2 = mouseEvent.getY();
                if (TelaInicial.this.pagina == Pagina.INICIO) {
                    for (int i = 0; i < AREAS_MENU.length; ++i) {
                        if (!AREAS_MENU[i].contains(n, n2)) continue;
                        if(TelaInicial.this.opcao!=i)GuitarraMenu.passar(i);TelaInicial.this.opcao = i;
                        TelaInicial.this.acionarMenu(i);
                        break;
                    }
                } else if (TelaInicial.this.pagina == Pagina.HANGAR) {
                    boolean bl = false;
                    for (int i = 0; i < AREAS_HANGAR.length; ++i) {
                        if (!AREAS_HANGAR[i].contains(n, n2)) continue;
                        TelaInicial.this.modeloSelecionado = i;
                        bl = true;
                        break;
                    }
                    if (!bl && n2 >= 552) {
                        if (n >= 655) {
                            TelaInicial.this.iniciarPartida();
                        } else if (n < 190) {
                            TelaInicial.this.pagina = Pagina.INICIO;
                        }
                    }
                } else if (TelaInicial.this.pagina == Pagina.TROFEUS) {
                    if (n > 62 && n < 610 && n2 > 165 && n2 < 520) {
                        int n3 = Math.min(3, (n - 62) / 140);
                        int n4 = Math.min(1, (n2 - 175) / 160);
                        TelaInicial.this.trofeuSelecionado = Medalhas.ordem()[n4 * 4 + n3];
                    } else if (n2 > 545) {
                        TelaInicial.this.pagina = Pagina.INICIO;
                    }
                } else {
                    TelaInicial.this.pagina = Pagina.INICIO;
                }
                TelaInicial.this.repaint();
            }
        });
    }

    private void acionarMenu(int n) {GuitarraMenu.confirmar(n);
        switch (n) {
            case 0: {
                this.iniciarPartida();
                break;
            }
            case 1: {
                this.pagina = Pagina.HANGAR;
                break;
            }
            case 2: {
                this.pagina = Pagina.TROFEUS;
                break;
            }
            case 3: {
                PainelSom.abrir();
                break;
            }
            case 4: {
                this.pagina = Pagina.CREDITOS;
                break;
            }
            case 5: {
                this.janela.dispose();
                break;
            }
        }
    }

    private void iniciarPartida() {
        if (!ProgressoJogo.modeloDisponivel(this.modeloSelecionado)) {
            this.pagina = Pagina.HANGAR;
            return;
        }
        this.animador.stop();
        Fase fase = new Fase();
        fase.selecionarModeloNave(this.modeloSelecionado);
        this.janela.setContentPane(fase);
        this.janela.pack();
        this.janela.setLocationRelativeTo(null);
        SwingUtilities.invokeLater(fase::requestFocusInWindow);
        MusicaJogo.trocarSeDisponivel(Recompensas.trilhaDoSetor(0));
    }

    private void desenharHangar(Graphics2D graphics2D) {
        String[] stringArray;
        graphics2D.setColor(new Color(3, 8, 19));
        graphics2D.fillRect(0, 0, 960, 600);
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        int n = this.arteHangar.getWidth(null);
        int n2 = this.arteHangar.getHeight(null);
        graphics2D.drawImage(this.arteHangar, 0, 0, 960, 116, 0, 0, n, (int)((double)n2 * 60.0 / 311.0), null);
        int[][] nArrayArray = new int[][]{{14, 60, 126, 214}, {131, 94, 225, 230}, {234, 95, 330, 230}, {341, 95, 438, 230}, {444, 60, 551, 230}};
        for (int i = 0; i < 5; ++i) {
            Rectangle card = AREAS_HANGAR[i];
            int n3 = card.x;
            boolean bl = ProgressoJogo.modeloDisponivel(i);
            boolean bl2 = i == this.modeloSelecionado;
            Color color = bl2 ? new Color(72, 224, 255) : new Color(42, 78, 111);
            graphics2D.setColor(new Color(7, 17, 32));
            graphics2D.fillRoundRect(n3, card.y, card.width, card.height, 12, 12);
            graphics2D.setStroke(new BasicStroke(bl2 ? 2.0f : 1.0f));
            graphics2D.setColor(color);
            graphics2D.drawRoundRect(n3, card.y, card.width, card.height, 12, 12);
            if (bl2) {
                graphics2D.fillRoundRect(n3 + 40, 126, 100, 4, 3, 3);
            }
            int[] nArray = nArrayArray[i];
            int n4 = nArray[0] * n / 559;
            int n5 = nArray[1] * n2 / 311;
            int n6 = nArray[2] * n / 559;
            int n7 = nArray[3] * n2 / 311;
            double d = Math.min(152.0 / (double)(n6 - n4), 194.0 / (double)(n7 - n5));
            int n8 = (int)((double)(n6 - n4) * d);
            int n9 = (int)((double)(n7 - n5) * d);
            int n10 = n3 + 90 - n8 / 2;
            int n11 = 139 + (194 - n9) / 2;
            graphics2D.drawImage(this.arteHangar, n10, n11, n10 + n8, n11 + n9, n4, n5, n6, n7, null);
            TelaInicial.texto(graphics2D, NOMES_NAVES[i], n3 + 12, 352, 16, Color.WHITE);
            TelaInicial.texto(graphics2D, bl ? "LIBERADA" : "BLOQUEADA", n3 + 12, 376, 14, bl ? new Color(109, 239, 174) : new Color(255, 165, 112));
            TelaInicial.texto(graphics2D, i == 0 ? "NO INÍCIO" : (i == 4 ? "ZERAR O JOGO" : REQUISITOS[i]), n3 + 12, 398, 13, new Color(170, 194, 218));
            graphics2D.setColor(new Color(42, 78, 111));
            graphics2D.drawLine(n3 + 12, 405, n3 + 168, 405);
            String[] nomesAtr = {"VELOC.", "PODER", "CASCO", "DEFESA", "TIROS"};
            for (int a = 0; a < 5; a++) TelaInicial.atributoHangar(graphics2D, nomesAtr[a], segmentos(a, i), n3, 416 + a * 19, bl2);
        }
        PerfilNave perfilNave = PerfilNave.obter(this.modeloSelecionado);
        TelaInicial.texto(graphics2D, perfilNave.funcao + "  |  ESCUDO: " + perfilNave.escudo + " S", 20, 538, 13, new Color(187, 211, 234));
        stringArray = new String[]{"Versatil: combina casco, mobilidade e tiro sem extremos.", "340 px/s e tiro rapido; casco de 9 exige esquiva precisa.", "Tiro rapido e escudo de 26 segundos; proteja-se nos confrontos longos.", "16 de casco e dano dobrado por tiro; menor velocidade e cadencia.", "Dano dobrado, 16 de casco e blindagem: -1 por impacto, com minimo de 1."};
        TelaInicial.texto(graphics2D, stringArray[this.modeloSelecionado], 20, 560, 13, new Color(134, 169, 198));
        boolean livre = ProgressoJogo.modeloDisponivel(this.modeloSelecionado);
        rodape(graphics2D, new String[]{"ESC: VOLTAR", "SETAS: ESCOLHER NAVE", livre ? "ENTER: INICIAR" : "NAVE BLOQUEADA", "F9: OPÇÕES"},
                new Color[]{Color.WHITE, Color.WHITE, livre ? new Color(109, 239, 174) : new Color(255, 165, 112), new Color(120, 220, 255)});
    }

    /** Atributo do cartao: nome em fonte pixel e uma barra de 5 gomos (comparando as 5 naves). */
    private static void atributoHangar(Graphics2D g, String nome, int gomos, int n, int y, boolean selecionada) {
        TelaInicial.texto(g, nome, n + 12, y + 14, 13, new Color(147, 179, 207));
        for (int k = 0; k < 5; k++) {
            g.setColor(k < gomos ? (selecionada ? new Color(72, 224, 255) : new Color(150, 190, 220)) : new Color(30, 44, 62));
            g.fillRect(n + 112 + k * 12, y + 2, 10, 10);
        }
    }

    /** De 1 a 5 gomos: onde a nave fica entre a pior e a melhor das 5 nesse atributo. */
    private static int segmentos(int atributo, int nave) {
        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE, v = 0;
        for (int i = 0; i < 5; i++) {
            PerfilNave p = PerfilNave.obter(i);
            double x = atributo == 0 ? p.velocidade : atributo == 1 ? p.dano : atributo == 2 ? p.casco : atributo == 3 ? p.defesa : 1.0 / p.intervalo;
            min = Math.min(min, x); max = Math.max(max, x); if (i == nave) v = x;
        }
        return max - min < 1e-9 ? 3 : 1 + (int) Math.round(4 * (v - min) / (max - min));
    }

    @Override
    public void removeNotify() {
        this.animador.stop();
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D graphics2D = (Graphics2D)graphics.create();
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        switch (this.pagina) {
            case INICIO: {
                graphics2D.drawImage(this.arteInicioNeutro, 0, 0, 960, 600, null);
                if (this.animado == null) this.animado = novoAnimado();
                this.animado.desenhar(graphics2D);
                this.desenharLuzesInicio(graphics2D);
                Rectangle rk = AREAS_MENU[4]; // a arte diz CREDITOS: cobre e escreve RANKING
                graphics2D.setColor(COR_BOTAO); graphics2D.fillRect(rk.x + 14, rk.y + 9, rk.width - 28, rk.height - 18);
                FontePixel.centro(graphics2D, "RANKING", rk.x + rk.width / 2, rk.y + rk.height / 2 - 10, 3, new Color(232, 238, 248));
                Rectangle rectangle = AREAS_MENU[this.opcao];
                graphics2D.setColor(new Color(174, 35, 7, (int)(65.0 + 45.0 * (1.0 + Math.sin(this.tempoHangar * 3.0)) / 2.0)));
                graphics2D.fillRoundRect(rectangle.x + 4, rectangle.y + 4, rectangle.width - 8, rectangle.height - 8, 9, 9);
                graphics2D.setStroke(new BasicStroke(4.0f));
                graphics2D.setColor(new Color(255, 86, 35, (int)(65.0 + 95.0 * (1.0 + Math.sin(this.tempoHangar * 3.0)) / 2.0)));
                graphics2D.drawRoundRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height, 9, 9);
                graphics2D.setStroke(new BasicStroke(2.25f));
                graphics2D.setColor(new Color(255, 142, 73, 233));
                graphics2D.drawRoundRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height, 9, 9);
                break;
            }
            case HANGAR: {
                this.desenharHangar(graphics2D);
                break;
            }
            case TROFEUS: {
                int n;
                graphics2D.drawImage(this.arteTrofeus, 0, 0, 960, 600, null);
                graphics2D.setColor(new Color(2, 8, 20));
                graphics2D.fillRect(28, 0, 915, 64);
                TelaInicial.texto(graphics2D, "METAL SPACE  /  CONQUISTAS", 68, 43, 27, new Color(255, 225, 177));
                graphics2D.setColor(new Color(5,12,24));
                graphics2D.fillRoundRect(28, 64, 915, 470, 14, 14);
                graphics2D.setStroke(new BasicStroke(2.0f));
                graphics2D.setColor(new Color(63, 149, 204));
                graphics2D.drawRoundRect(28, 64, 915, 470, 14, 14);
                TelaInicial.texto(graphics2D, "RELIQUIAS DOS UNIVERSOS", 67, 113, 26, new Color(235, 243, 253));
                TelaInicial.texto(graphics2D, "PODERES DOS CHEFES / CHAVES PARA O PROXIMO UNIVERSO", 67, 145, 14, new Color(149, 192, 225));
                graphics2D.setColor(new Color(55, 104, 147));
                graphics2D.drawLine(49, 161, 925, 161);
                int[] nArray = Medalhas.ordem();
                for (n = 0; n < nArray.length; ++n) {
                    int n2 = 66 + n % 4 * 141;
                    int n3 = 178 + n / 4 * 163;
                    int n4 = nArray[n];
                    Medalhas.desenhar(graphics2D, n4, ProgressoJogo.temTrofeu(n4), n2, n3, 103, -1.0);
                    if (n4 != this.trofeuSelecionado) continue;
                    graphics2D.setColor(new Color(246, 165, 84));
                    graphics2D.setStroke(new BasicStroke(2.0f));
                    graphics2D.drawRoundRect(n2 - 3, n3 - 3, 109, 109, 12, 12);
                }
                graphics2D.setColor(new Color(3, 9, 20, 237));
                graphics2D.fillRoundRect(654, 165, 290, 324, 12, 12);
                n = ProgressoJogo.temTrofeu(this.trofeuSelecionado) ? 1 : 0;
                Medalhas.desenhar(graphics2D, this.trofeuSelecionado, n != 0, 735, 193, 132, -1.0);
                TelaInicial.texto(graphics2D, n != 0 ? Recompensas.nome(this.trofeuSelecionado) : "INSIGNIA BLOQUEADA", 670, 371, 16, Color.WHITE);
                textoQuebrado(graphics2D, n != 0 ? Recompensas.habilidade(this.trofeuSelecionado) : "DERROTE O CHEFE", 670, 392, 2, new Color(175, 208, 231), 262);
                TelaInicial.texto(graphics2D, "CONQUISTAS: " + ProgressoJogo.totalTrofeus() + "/8", 672, 463, 16, Color.ORANGE);
                graphics2D.setColor(new Color(3, 8, 19));
                graphics2D.fillRect(0, 536, 960, 64);
                rodape(graphics2D, new String[]{"ESC: VOLTAR", "SETAS: ESCOLHER INSÍGNIA", "F9: OPÇÕES"}, new Color[]{Color.WHITE, Color.WHITE, new Color(120, 220, 255)});
                break;
            }
            case CREDITOS: {
                graphics2D.drawImage(this.arteInicioNeutro, 0, 0, 960, 600, null);
                Placar.desenhar(graphics2D, placar.ler(), "ESC: VOLTAR");
                FontePixel.centro(graphics2D, "RÁDIO: FFMPEG (LGPL 3) - VEJA LICENCAS.TXT", 480, 562, 1, new Color(110, 120, 140));
                break;
            }
            case INFORMACOES: {
                graphics2D.drawImage(this.arteInicioNeutro, 0, 0, 960, 600, null);
                graphics2D.setColor(new Color(2, 8, 20, 243));
                graphics2D.fillRoundRect(135, 120, 725, 390, 15, 15);
                TelaInicial.texto(graphics2D, this.pagina == Pagina.INFORMACOES ? "CONTROLES" : "CREDITOS", 207, 183, 27, Color.WHITE);
                if (this.pagina == Pagina.INFORMACOES) {
                    TelaInicial.texto(graphics2D, "WASD / SETAS: PILOTAR", 207, 235, 19, Color.CYAN);
                    TelaInicial.texto(graphics2D, "ESPACO: ATIRAR     F: TROCAR ARMA", 207, 273, 19, Color.WHITE);
                    TelaInicial.texto(graphics2D, "ENTER/P/ESC: PAUSA  F8: TESTES", 207, 311, 19, Color.WHITE);
                    TelaInicial.texto(graphics2D, "Q: GELO / E: FOGO | V: CONVERGENCIA", 207, 359, 17, Color.ORANGE);
                } else {
                    TelaInicial.texto(graphics2D, "METAL SPACE - LEGENDS OF METAL", 207, 240, 21, Color.ORANGE);
                    TelaInicial.texto(graphics2D, "BASE ORIGINAL DO PROJETO DE FACULDADE", 207, 285, 16, Color.WHITE);
                    TelaInicial.texto(graphics2D, "VISUAL: CONCEITO APROVADO PELO CRIADOR", 207, 328, 16, Color.WHITE);
                    TelaInicial.texto(graphics2D, "A M\u00daSICA NUNCA MORRE NO ESPA\u00c7O", 207, 373, 17, Color.CYAN);
                    TelaInicial.texto(graphics2D, "RADIO: FFMPEG (LGPL 3) - VEJA LICENCAS.TXT", 207, 420, 13, new Color(150, 165, 185));
                }
                TelaInicial.texto(graphics2D, "ESC OU CLIQUE: VOLTAR", 207, 475, 17, Color.LIGHT_GRAY);
                break;
            }
        }
        if(pagina==Pagina.INICIO&&!PainelSom.aberto())desenharBarraInicio(graphics2D);
        if(verPlacar)Placar.desenhar(graphics2D,placar.ler(),"ESC / H: VOLTAR");
        PainelSom.desenhar(graphics2D);graphics2D.dispose();
    }

    private void desenharLuzesInicio(Graphics2D graphics2D) {
        int n;
        int n2;
        Graphics2D graphics2D2 = (Graphics2D)graphics2D.create();
        for (n2 = 0; n2 < 22; ++n2) {
            n = 24 + n2 * 43;
            double d = 0.5 + 0.5 * Math.sin(this.tempoHangar * 3.1 - (double)n2 * 0.65);
            graphics2D2.setColor(new Color(255, 55, 25, (int)(20.0 + d * 55.0)));
            graphics2D2.fillOval(n - 7, 8, 19, 19);
            graphics2D2.setColor(new Color(255, 100, 45, (int)(70.0 + d * 180.0)));
            graphics2D2.fillRect(n, 15, 5, 3);
        }
        graphics2D2.clipRect(18, 190, 572, 355);
        for (n2 = 0; n2 < 34; ++n2) {
            n = 25 + (int)(((double)(n2 * 97) + this.tempoHangar * (double)(18 + n2 % 4 * 13)) % 550.0);
            int n3 = 535 - (int)(((double)(n2 * 71) + this.tempoHangar * (double)(22 + n2 % 5 * 9)) % 330.0);
            graphics2D2.setColor(new Color(110, 205, 255, 40 + n2 % 5 * 22));
            graphics2D2.fillRect(n, n3, 2, 2 + n2 % 3);
        }
        graphics2D2.setClip(null);
        graphics2D2.dispose();
    }

    private FundoInicioAnimado novoAnimado() {
        Rectangle menu = new Rectangle(AREAS_MENU[0]);
        for (Rectangle r : AREAS_MENU) menu.add(r);
        menu.grow(12, 12);
        return new FundoInicioAnimado(this.arteInicioNeutro, menu, new Rectangle(0, 0, 960, 34), new Rectangle(0, 576, 960, 24));
    }

    /** Rodape da tela inicial: uma faixa so, em fonte pixel, com versao, placar, decolar (piscando) e atalhos. */
    private void desenharBarraInicio(Graphics2D g) {
        g.setColor(new Color(1, 6, 16, 225)); g.fillRect(0, 576, 960, 24);
        g.setColor(new Color(255, 120, 40, 170)); g.fillRect(0, 576, 960, 2);
        String[] t = {"0.9.8 ALPHA", "[H] RANKING", "ENTER: DECOLAR", "F9: OPÇÕES", "F10: MUDO"};
        Color[] c = {new Color(170, 185, 210), Color.WHITE, new Color(150, 232, 255), new Color(120, 220, 255), new Color(120, 220, 255)};
        int soma = 0; for (String s : t) soma += FontePixel.largura(s, 2);
        int vao = (960 - 32 - soma) / (t.length - 1), x = 16;
        for (int i = 0; i < t.length; i++) {
            boolean pisca = i == 2 && Math.sin(this.tempoHangar * 2.6) <= -0.4;
            if (!pisca) FontePixel.desenhar(g, t[i], x, 583, 2, c[i]);
            x += FontePixel.largura(t[i], 2) + vao;
        }
    }

    /** Texto com contorno preto de 1 pixel, legivel sobre a arte (usa a fonte ja escolhida). */
    /** Texto em fonte pixel; n2 e a linha de base (como no drawString) e n3 o tamanho antigo, que vira escala 1-3. */
    private static void texto(Graphics2D graphics2D, String string, int n, int n2, int n3, Color color) {
        int e = n3 >= 24 ? 3 : n3 >= 13 ? 2 : 1;
        FontePixel.desenhar(graphics2D, string, n, n2 - FontePixel.altura(e), e, color);
    }

    /** Texto em fonte pixel quebrado em linhas que cabem na largura; devolve o y depois da ultima linha. */
    private static int textoQuebrado(Graphics2D g, String s, int x, int y, int e, Color cor, int largura) {
        StringBuilder linha = new StringBuilder();
        for (String p : s.split(" ")) {
            String teste = linha.length() == 0 ? p : linha + " " + p;
            if (FontePixel.largura(teste, e) > largura && linha.length() > 0) { FontePixel.desenhar(g, linha.toString(), x, y, e, cor); y += FontePixel.altura(e) + 6; linha = new StringBuilder(p); }
            else linha = new StringBuilder(teste);
        }
        if (linha.length() > 0) { FontePixel.desenhar(g, linha.toString(), x, y, e, cor); y += FontePixel.altura(e) + 6; }
        return y;
    }

    /** Rodape de qualquer tela: uma faixa so, embaixo, com as teclas espalhadas (fonte pixel). */
    private static void rodape(Graphics2D g, String[] t, Color[] c) {
        g.setColor(new Color(1, 6, 16, 235)); g.fillRect(0, 572, 960, 28);
        g.setColor(new Color(255, 120, 40, 170)); g.fillRect(0, 572, 960, 2);
        int soma = 0; for (String s : t) soma += FontePixel.largura(s, 2);
        int vao = t.length > 1 ? (960 - 32 - soma) / (t.length - 1) : 0, x = t.length > 1 ? 16 : (960 - soma) / 2;
        for (int i = 0; i < t.length; i++) { FontePixel.desenhar(g, t[i], x, 581, 2, c[i]); x += FontePixel.largura(t[i], 2) + vao; }
    }

    public static void abrir() {
        JFrame jFrame = new JFrame("METAL SPACE - Legends of Metal | 0.9.8C ALPHA ENXAME");
        jFrame.setDefaultCloseOperation(3);
        // abertura (placa e logo acendendo) antes do menu; ENTER pula
        AberturaJogo abertura = new AberturaJogo(() -> SwingUtilities.invokeLater(() -> {
            TelaInicial telaInicial = new TelaInicial(jFrame);
            jFrame.setContentPane(telaInicial);
            jFrame.revalidate(); jFrame.repaint();
            telaInicial.requestFocusInWindow();
        }));
        // a musica da entrada e a da radio (estacao, playlist ou trilha, como o jogador deixou): liga ja,
        // antes da abertura, para nao comecar uma musica e trocar por outra no meio
        RadioMetal.retomar();
        jFrame.setContentPane(abertura);
        jFrame.pack();
        jFrame.setResizable(false);
        jFrame.setLocationRelativeTo(null);
        jFrame.setVisible(true);
        SwingUtilities.invokeLater(abertura::comecar);
    }

    private static enum Pagina {
        INICIO,
        HANGAR,
        TROFEUS,
        INFORMACOES,
        CREDITOS;

    }
}

