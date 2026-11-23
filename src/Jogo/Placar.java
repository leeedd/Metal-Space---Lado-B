package Jogo;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.prefs.*;
/** Placar local independente do saldo de creditos. */
final class Placar {
 static final class Entrada {
  final String iniciais,modo; final int pontos;
  Entrada(String i,int p,String m){iniciais=i;pontos=p;modo=m;}
 }
 private final Preferences dados;
 Placar(){this(Preferences.userNodeForPackage(Placar.class).node("placar095"));}
 Placar(Preferences p){dados=p;}
 List<Entrada> ler(){
  List<Entrada> lista=new ArrayList<>();
  try{for(int i=0;i<10;i++){String v=dados.get("r"+i,"");String[] s=v.split(";");if(s.length!=3)continue;
   try{int p=Integer.parseInt(s[1]);if(p>0&&s[0].matches("[A-Z0-9]{3}"))lista.add(new Entrada(s[0],p,s[2]));}catch(NumberFormatException ignored){}
  }}catch(SecurityException ignored){}
  lista.sort((a,b)->Integer.compare(b.pontos,a.pontos));return lista;
 }
 boolean salvar(String iniciais,int pontos,String modo){
  if(pontos<=0||!iniciais.matches("[A-Z0-9]{3}"))return false;
  List<Entrada> l=ler();l.add(new Entrada(iniciais,pontos,modo));l.sort((a,b)->Integer.compare(b.pontos,a.pontos));
  try{for(int i=0;i<Math.min(10,l.size());i++){Entrada e=l.get(i);dados.put("r"+i,e.iniciais+";"+e.pontos+";"+e.modo);}dados.flush();return true;}
  catch(SecurityException|BackingStoreException ex){return false;}
 }
 /** Ranking das lendas (tela RANKING e tecla H): o 1o lugar em destaque dourado, brilhando, com trofeu;
  *  2o e 3o com trofeu de prata e bronze; tudo em fonte pixel. O trofeu e provisorio (desenhado no codigo). */
 static void desenhar(Graphics2D tela,List<Entrada> lista,String rodape){
  Graphics2D g=(Graphics2D)tela.create();
  long agora=System.currentTimeMillis();double pulso=.5+.5*Math.sin(agora/260.0);
  Color ouro=new Color(255,204,64),ouroClaro=new Color(255,236,150),prata=new Color(200,210,226),bronze=new Color(214,128,64),texto=new Color(226,232,244),cinza=new Color(140,150,172);
  g.setColor(new Color(0,0,0,170));g.fillRect(0,0,960,600);
  g.setColor(new Color(4,7,18,245));g.fillRect(120,24,720,552);
  g.setColor(new Color(150,110,30));g.fillRect(120,24,720,4);g.fillRect(120,572,720,4);g.fillRect(120,24,4,552);g.fillRect(836,24,4,552);
  FontePixel.centro(g,"RANKING DAS LENDAS",480,44,4,ouro);
  if(lista.isEmpty()){FontePixel.centro(g,"NINGUÉM AINDA. SEJA O PRIMEIRO!",480,280,3,texto);}
  else{
   // 1o lugar: caixa dourada com brilho pulsando e um reflexo passando
   Entrada p=lista.get(0);int bx=160,by=104,bw=640,bh=128;
   for(int k=3;k>=1;k--){g.setColor(new Color(255,190,40,(int)((18+22*pulso)*(4-k)/3)));g.fillRect(bx-k*6,by-k*6,bw+k*12,bh+k*12);}
   g.setColor(new Color(40,28,6));g.fillRect(bx,by,bw,bh);
   g.setColor(ouro);g.fillRect(bx,by,bw,4);g.fillRect(bx,by+bh-4,bw,4);g.fillRect(bx,by,4,bh);g.fillRect(bx+bw-4,by,4,bh);
   Graphics2D r=(Graphics2D)g.create();r.clipRect(bx+4,by+4,bw-8,bh-8);int rx=bx-120+(int)((agora/6)%(bw+240));
   r.setColor(new Color(255,245,200,55));r.fillRect(rx,by,10,bh);r.fillRect(rx+16,by,4,bh);r.dispose();
   trofeu(g,bx+20,by+24,8,ouro,ouroClaro);
   FontePixel.desenhar(g,"CAMPEÃO",bx+130,by+18,2,ouroClaro);
   FontePixel.desenhar(g,"1",bx+130,by+48,7,ouro);
   FontePixel.desenhar(g,p.iniciais,bx+190,by+48,7,ouroClaro);
   String pts=String.format(Locale.ROOT,"%,d",p.pontos).replace(',','.');
   FontePixel.desenhar(g,"PONTOS",bx+bw-24-FontePixel.largura("PONTOS",2),by+18,2,ouroClaro);
   FontePixel.desenhar(g,pts,bx+bw-24-FontePixel.largura(pts,5),by+54,5,ouro);
   // demais: posicao, piloto e pontuacao geral
   FontePixel.desenhar(g,"POS",184,256,2,cinza);FontePixel.desenhar(g,"PILOTO",300,256,2,cinza);
   FontePixel.desenhar(g,"PONTUAÇÃO GERAL",776-FontePixel.largura("PONTUAÇÃO GERAL",2),256,2,cinza);
   for(int i=1;i<lista.size();i++){Entrada e=lista.get(i);int y=282+(i-1)*28;Color c=i==1?prata:i==2?bronze:texto;int es=i<=2?3:2;
    if(i%2==1){g.setColor(new Color(255,255,255,12));g.fillRect(172,y-6,616,28);}
    if(i<=2)trofeu(g,244,y-3,2,c,c.brighter());
    FontePixel.desenhar(g,String.format(Locale.ROOT,"%02d",i+1),184,y,es,c);
    FontePixel.desenhar(g,e.iniciais,300,y,es,c);
    String s=String.format(Locale.ROOT,"%,d",e.pontos).replace(',','.');
    FontePixel.desenhar(g,s,776-FontePixel.largura(s,es),y,es,c);}
  }
  FontePixel.centro(g,rodape,480,546,2,new Color(120,220,255));
  g.dispose();
 }
 /** Trofeu em pixel art (provisorio, ate a arte do GPT): taca com alcas, haste e base, em blocos de 'e' px. */
 static void trofeu(Graphics2D g,int x,int y,int e,Color cor,Color luz){
  String[] m={"##.......##","#.#######.#","#.#######.#",".#.#####.#.","...#####...","....###....",".....#.....","....###....","..#######.."};
  Color sombra=cor.darker();int b=Math.max(1,e/2);
  g.setColor(new Color(10,8,4));
  for(int l=0;l<m.length;l++)for(int c=0;c<m[l].length();c++)if(m[l].charAt(c)=='#')g.fillRect(x+c*e-b,y+l*e-b,e+2*b,e+2*b);
  for(int l=0;l<m.length;l++)for(int c=0;c<m[l].length();c++)if(m[l].charAt(c)=='#'){g.setColor(c<=3&&l<5?luz:c>=7?sombra:cor);g.fillRect(x+c*e,y+l*e,e,e);}
 }
}
