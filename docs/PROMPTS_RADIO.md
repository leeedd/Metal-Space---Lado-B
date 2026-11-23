# Rádio de bordo e tela de entrada: artes para o GPT

Hoje a rádio (F7 ou Opções > Áudio > Rádio) e o movimento da tela de entrada são desenhados pelo código.
Com estas artes o jogo passa a usar as suas, e os controles continuam iguais (girar, clicar, teclado).
Cada arte é um arquivo PNG separado, com o nome indicado. O jogo escreve os textos (estação, música, números do dial),
então **nenhuma arte leva texto**.

**Regras fixas (copie no começo de todo prompt):**
```
Pixel art 16-bit, estilo SNES/arcade dos anos 90, contorno escuro de 1 pixel, sombreado em 3 a 4 tons por cor, sem antialias e sem degradê suave. Fundo magenta sólido #FF00FF. Sem texto, sem letras, sem logotipos e sem marcas. Personagens ORIGINAIS, sem semelhança com pessoas, bandas, mascotes ou personagens reais.
```

**Estilo do rádio: é o rádio de bordo da própria nave.** Feito com as mesmas peças da nave da tela inicial:
placas de casco **vermelho** com bordas **cromadas**, painéis **pretos** com rebites e o **azul elétrico** dos motores
como luz de neon. Formato de rádio retrô (alto-falante com grade, dial, dois botões de girar), como se fosse um
toca-fitas dos anos 80 montado no painel do cockpit. Visto **de frente, reto**, sem perspectiva.

**Onde ele fica:** embutido no **centro do painel do cockpit** (as artes `cockpit_1.png` a `cockpit_4.png`), preso por
suportes nas laterais, como um toca-fitas de carro. O jogador vê o cockpit em volta e o espaço pelo para-brisa.
A **mão enluvada do piloto** (a mesma da `maos_piloto.png`) aperta os botões e gira os knobs.

**Dica:** mande junto, para o GPT, um print da tela inicial (a nave vermelha e cromada) e diga "use as cores e o
acabamento desta nave".

---

## 1. `radio_corpo.png`: o aparelho vazio (1 imagem, cerca de 760x480)
> Rádio de bordo de uma nave espacial, visto de frente, reto, sem perspectiva, proporção 16:10, com o mesmo acabamento da nave da imagem de referência: placas de casco vermelho com bordas cromadas e chanfradas, painéis pretos com rebites e luzes de neon azul elétrico como as dos motores. Formato de rádio retrô dos anos 80 embutido no painel do cockpit, SEM alça, com dois suportes metálicos parafusados nas laterais prendendo o aparelho ao painel; na metade de cima um alto-falante largo com grade de frisos horizontais pretos, emoldurado em cromo, com um fino neon azul em volta; logo abaixo uma janela de dial larga e VAZIA, vidro azul-marinho bem escuro com moldura cromada (sem números, sem ponteiro: o jogo desenha). Na parte de baixo, dois encaixes redondos vazios para botões de girar, um de cada lado, e no meio um painel preto liso para os botões. Pequenas saídas de ventilação e rebites nas laterais vermelhas. Sem botões, sem texto.

## 2. `radio_knob.png`: botão de girar cromado (folha com 8 quadros, cada um cerca de 96x96)
> Folha horizontal com 8 quadros do mesmo tamanho, lado a lado, sem divisórias: um botão de girar redondo cromado visto de cima, no estilo das peças da nave (cromo com anel vermelho), com borda serrilhada, brilho de cromo no canto de cima à esquerda e uma marca azul-elétrica acesa indicando a posição. Em cada quadro a marca gira 45 graus no sentido horário (o brilho do cromo fica parado, só a marca e o serrilhado giram).

## 3. `radio_botao.png`: tecla de apertar (folha com 3 quadros, cada um cerca de 60x40)
> Folha horizontal com 3 quadros do mesmo tamanho: uma tecla retangular de painel de nave, preta com borda cromada, tampa levemente mais clara em cima e sombra embaixo. Quadro 1: normal. Quadro 2: selecionada, com contorno dourado. Quadro 3: apertada (afundada 2 pixels) com um pequeno LED azul-elétrico aceso no canto de cima à direita. Sem números nem letras.

## 4. `radio_chave.png`: chave deslizante de 3 posições (folha com 2 quadros)
> Folha horizontal com 2 quadros: quadro 1 é o trilho da chave, uma canaleta preta comprida (cerca de 320x28) com moldura cromada; quadro 2 é o cursor da chave, um bloco vermelho de casco com bordas cromadas (cerca de 100x24) e frisos verticais para o dedo.

## 5. `radio_ponteiro.png`: ponteiro do dial (1 imagem, cerca de 12x50)
> Ponteiro vertical fino vermelho-vivo de dial de rádio antigo, com um pequeno brilho rosado no topo e um leve halo vermelho em volta.

## 6. `maos_radio.png`: a mão do piloto mexendo no rádio (folha com 5 quadros, cada um cerca de 200x260)
> Folha horizontal com 5 quadros do mesmo tamanho, lado a lado, sem divisórias: a mão direita do piloto em primeira pessoa, entrando por baixo da tela, com o mesmo visual da folha maos_piloto.png (luva de couro marrom, tira com fivela no pulso, manga laranja do macacão de voo). Sem nenhum objeto na mão. Quadro 1: dedo indicador esticado apontando para cima (para apertar botões). Quadro 2: o mesmo dedo dobrado, apertando (a mão 4 pixels mais para cima). Quadro 3: mão aberta em garra, dedos curvados para baixo, como quem vai segurar um botão redondo de girar visto de cima. Quadros 4 e 5: a mesma garra girada para a direita (como girando o botão), um pouco mais a cada quadro.

---

## 7. Tela de entrada com a nave se mexendo
Hoje a nave está grudada no fundo (`ui_inicio_neutro.png`), por isso não se mexe sozinha. Com estas duas artes ela flutua
e os jatos pulsam de verdade.

### `ui_inicio_fundo.png`: o fundo sem a nave (1 imagem, 960x600)
> A mesma cena da tela inicial do Metal Space (espaço com nebulosa roxa, planeta azul à esquerda, asteroides, o logo METAL SPACE no alto e a coluna de botões à direita), mas SEM a nave: onde ela estava, continua o espaço e o planeta.

### `nave_inicio.png`: a nave da tela inicial (folha com 4 quadros)
> Folha horizontal com 4 quadros do mesmo tamanho, lado a lado, sem divisórias: a nave vermelha e cromada da tela inicial do Metal Space, apontando para cima e para a direita, com os jatos azuis dos motores saindo para trás. Nos 4 quadros a nave é idêntica; só os jatos mudam (curto, médio, longo, médio), para animar o fogo dos motores.

---

Mande cada arte como **arquivo** com o nome acima. Se o GPT errar o tamanho, sem problema: o jogo ajusta,
desde que as folhas tenham os quadros do mesmo tamanho, lado a lado.
