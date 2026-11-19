# Metal-Space---Lado-B
Lado B - Versão para ser distribuida.

## Rádio do jogo

Em **Opções (F9) > Áudio > Rádio** o jogador escolhe a música de fundo: a trilha do jogo, uma das rádios de metal
da internet ou a própria playlist. **F11** passa para a próxima estação/música e **F12** volta, em qualquer tela.
O nome da rádio, a música e o artista aparecem no canto da tela a cada troca.

- **Playlist:** coloque as músicas em `musicas/playlist` (ao lado do jogo). Toca qualquer formato que o FFmpeg leia
  (MP3, FLAC, OGG, OPUS, M4A/AAC, WAV, WMA, AIFF...). Artista e música vêm das tags; sem tags, do nome
  `Artista - Música.mp3`.
- **Rádios do jogador:** na tela da rádio, copie o link da estação e aperte **V**; **DEL** remove.
- **FFmpeg:** a rádio usa o `ffmpeg/ffmpeg.exe` (LGPL). Ele tem mais de 100 MB e não vai para o GitHub:
  em cada computador, rode uma vez `baixar_ffmpeg.ps1`. Sem ele o jogo funciona normal e a rádio avisa.
- **Testes:** `TesteRadio` (sem internet: rádio falsa local, formatos, falhas) e `TesteRadiosAoVivo`
  (confere se as rádios da lista estão no ar).
- Licenças de terceiros: `LICENCAS.txt`.
