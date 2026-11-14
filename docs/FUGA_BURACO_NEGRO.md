# Fuga do Black Hole (fase final especial)

Depois do último chefe, o inferno desaba e a nave sobe pelo rio de lava até a saída, em primeira pessoa, dentro da cabine. No fim vêm a gárgula colossal e a dobra espacial. Depois, os créditos.

## O que o projeto já tinha
- **Motor:** Java 8 com Swing/Java2D (`Fase` é um `JPanel`), 960x600 a 60 quadros. Não há 3D, então a fuga usa pseudo-3D: projeção em perspectiva, escala de sprites e fatias do corredor.
- **Fluxo das fases:** `Fase.Etapa` (COMBATE, BATALHA_CHEFE, INTERLUDIO, CREDITOS...). A fuga entra em `atualizarTransicao`, depois de `finalVencido` e antes de `CREDITOS`.
- **Naves:** 5 selecionáveis (`TelaInicial.NOMES_NAVES`): 0 RAPTOR MK-I, 1 SHADOW, 2 VALKYRIE, 3 BERSERKER (a militar verde) e 4 METAL GOD. O índice vem de `nave.getModeloVisual()`.
- **Controles:** setas/WASD movem, ESPAÇO atira, Q/E dão o tiro elemental, SHIFT dá o impulso, C usa o poder, X troca de poder e ENTER/P/ESC pausam.
- **Armas e dano:** `Nave` e `Missel`, bomba nuclear em `Fase`, dano via `processarDano`.
- **Áudio:** `SomJogo` (efeitos em `recursos/Audio098`) e `MusicaJogo` (troca de trilha).
- **Cutscenes:** classes próprias (`CenaPouso`, `CenaFinal`, `EntradaChefe`).
- **Cabines existentes:** `cabine_0..4.png` são vistas por cima do ombro, com a cabeça do piloto, usadas no pouso. Não servem para a primeira pessoa.

## Arquitetura
| Parte | Classe | Etapa |
|---|---|---|
| Gerenciamento da fase, aceleração, armas, danos | `FugaFinal` | 1 (feito) |
| Mundo pseudo-3D, obstáculos, gárgulas, tiros | `FugaPista` | 1 (feito) |
| Cockpit, mãos, HUD, radar, mapa, diagnóstico, transição de câmera | `FugaCockpit` | 1 (feito) |
| IA das naves inimigas em fuga | `FugaInimigos` | 2 |
| Gárgula colossal, carga da dobra, contagem, comando final | `FugaGargula` | 3 |
| Cutscene externa (braço rompendo) e hiperespaço no cockpit | `FugaCenas` | 3 |
| Computador de bordo (falas sincronizadas) | `FugaComputador` | 3 |
| Cockpits definitivos das 5 naves | arte + `FugaCockpit` | 4 |

## Controles na fuga
- Setas: manche (A/D também viram).
- W/S: alavanca de potência.
- ESPAÇO: canhões.
- F: míssil, que trava no alvo mais próximo.
- B: bomba.
- ENTER/P/ESC: pausa.
- Comando final (etapa 3): segurar ↑ e W juntos (manche todo à frente, potência máxima).

## Etapa 1 (feita): protótipo jogável
- **Transição:** a câmera se aproxima da nave e escurece, e o jogo abre dentro da cabine.
- **Aceleração livre:**
  - A alavanca vai de 0 a 100%, e a velocidade acompanha com inércia.
  - Quanto mais rápido, mais tremor, obstáculos chegando mais cedo, manobra mais dura e batidas mais fortes.
  - O colapso sobe atrás da nave. Quem anda devagar demais é alcançado e se fere.
- **Obstáculos:**
  - Rochas despencando, colunas de fogo, bolhas de lava em arco, gárgulas que perseguem a nave e paredes que desmoronam e fecham metade do corredor.
  - Ficam mais densos à medida que a nave sobe.
- **Danos:**
  - Seis sistemas: estrutura, escudos, motores, armas, navegação e radar.
  - O escudo absorve primeiro. O que passa racha a estrutura e fere um sistema.
  - Consequências: motor ferido limita a potência, arma ferida trava, navegação ferida deixa o manche mole e o mapa falha, radar ferido dá estática e perde alvos, e a estrutura ferida racha o vidro.
  - Temperatura alta e voo rente à lava também ferem.
- **Instrumentos:** velocímetro, potência (com o limite do motor), altitude, temperatura, canhão (munição e calor), mísseis (recarga), bombas, escudo, carga da dobra, radar (frente, lados e o colapso por trás, com alerta de ameaça), mapa da subida (setores, saída e colapso) e diagnóstico na silhueta da nave (verde, amarelo, laranja e vermelho).
- **Mãos:** a esquerda empurra a alavanca e o polegar abre a chave da bomba. A direita segura o manche, que inclina para os lados e para frente e para trás. O indicador puxa o gatilho e o polegar aperta o botão do míssil.
- **Falha:** a nave perdida volta ao último setor vencido (a subida tem 4 setores).

## Etapa 2: naves inimigas fugindo
Quatro comportamentos:
- **Competidor:** ultrapassa.
- **Agressor:** atira.
- **Sabotador:** fecha a passagem.
- **Desesperado:** perde o controle e cai.

Elas aparecem no radar e podem escapar ou ser destruídas pelo jogador, pelo ambiente ou pelas criaturas.

## Etapa 3: clímax
1. A gárgula colossal agarra a nave e puxa para a lava.
2. A dobra termina de carregar e o computador faz a contagem.
3. Aparece o comando do jogador.
4. Cutscene externa: o braço racha e explode, e a nave entra em dobra.
5. Tela preta.
6. Volta ao cockpit no hiperespaço, com a turbulência diminuindo, e o computador confirma a saída.
7. Créditos.

A partir do retorno à cabine, a câmera não sai mais dela.

## Artes que faltam
Todas seguem a regra fixa do `ARTE_LADO_B.md`: pixel art 16-bit, sem pessoas ou marcas reais, e fundo magenta onde indicado.

**Cockpits em primeira pessoa (5)**, `cockpit_N.png`, 960x600, de N=0 a 4:
- O vidro deve ser preto puro, que vira janela, e o painel ocupa a faixa de baixo (y ≥ 395).
- Sem piloto, sem rosto e sem mãos.
- O manche (centro-direita) e a alavanca (esquerda) ficam fora da arte, porque são animados por cima.
- Visual de cada nave:
  - 0 Raptor: branco e vermelho, painéis verdes.
  - 1 Shadow: preto com luz vermelha discreta, telas digitais.
  - 2 Valkyrie: branco e dourado com asas e cristais azuis.
  - 3 Berserker: blindado verde-oliva, rebites e mostradores industriais.
  - 4 Metal God: preto e dourado, caveiras, espinhos e pentagrama (sem símbolo religioso real).

**`maos_piloto.png`** (fundo magenta), luvas de piloto com mangas de macacão laranja, sem rosto:
- Linha 1, mão esquerda na alavanca, 3 quadros: segurando, empurrando para frente e o polegar abrindo a chave da bomba.
- Linha 2, mão direita no manche, 6 quadros: neutro, para esquerda, para direita, puxando o gatilho, polegar no botão do míssil e empurrando tudo à frente (comando final).

**Etapa 2:** `fuga_naves_inimigas.png` (fundo magenta), 4 modelos de nave inimiga vistos de trás, 3 quadros cada (reta, inclinada e avariada em chamas). As silhuetas devem ser diferentes das naves do jogador.

**Etapa 3:**
- `gargula_colossal.png` (fundo magenta), de frente, enorme, ocupando a janela: aparecendo, estendendo o braço e com a mão fechando.
- `gargula_mao.png`: a mão de pedra segurando a nave, de dentro da cabine, cobrindo parte do vidro, rachando em 3 estágios.
- `gargula_braco_rompendo.png`: vista externa, 6 quadros. A nave presa na mão, os motores no máximo, o braço rachando, explodindo, a nave se soltando e entrando em dobra.
- `hiperespaco_tunel.png`: o túnel de dobra, 4 quadros em ciclo, para ver pelo vidro.

**Áudio:**
- Voz robótica do computador de bordo, um WAV por fala: "Dobra espacial em cinco segundos", "Cinco", "Quatro", "Três", "Dois", "Um", "Entrando em dobra espacial!", "Instabilidade dimensional detectada!", "Saindo da influência gravitacional do Black Hole!", "Black Hole superado.", "Dobra espacial estabilizada.". O ambiente de desenvolvimento não tem sintetizador de voz. Até os arquivos chegarem, as falas aparecem na tela com bipes.
- `motor_cabine.wav`: loop do motor, cujo volume e tom sobem com a potência.
- `alarme_cabine.wav`: alarme da cabine.
- `trilha fuga.wav`: música da fase.
