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
 /** Ranking das lendas (tela RANKING e tecla H), no estilo dos botoes do menu: paineis azul-escuros com
  *  borda azul chanfrada e letras claras. O 1o lugar fica em destaque dourado, brilhando, com trofeu;
  *  2o e 3o com trofeu de prata e bronze. O trofeu e provisorio (desenhado no codigo). */
 static final Color FUNDO=new Color(3,11,23),BORDA=new Color(58,118,170),BORDA_ESCURA=new Color(26,50,80),LETRA=new Color(226,238,248),ROTULO=new Color(120,160,200);
 static void desenhar(Graphics2D tela,List<Entrada> lista,String rodape){
  Graphics2D g=(Graphics2D)tela.create();
  long agora=System.currentTimeMillis();double pulso=.5+.5*Math.sin(agora/260.0);
  Color ouro=new Color(255,204,64),ouroClaro=new Color(255,236,150),prata=new Color(200,210,226),bronze=new Color(214,128,64);
  g.setColor(new Color(0,0,0,170));g.fillRect(0,0,960,600);
  painel(g,120,20,720,560,16,FUNDO,BORDA,3);
  FontePixel.centro(g,"RANKING",480,40,4,LETRA);
  g.setColor(BORDA_ESCURA);g.fillRect(300,80,360,2);
  if(lista.isEmpty()){FontePixel.centro(g,"NINGUÉM AINDA. SEJA O PRIMEIRO!",480,280,3,LETRA);}
  else{
   // 1o lugar: painel com borda dourada pulsando e um reflexo passando
   Entrada p=lista.get(0);int bx=160,by=98,bw=640,bh=128;
   for(int k=3;k>=1;k--)painel(g,bx-k*5,by-k*5,bw+k*10,bh+k*10,14+k*5,new Color(255,190,40,(int)((14+20*pulso)*(4-k)/3)),null,0);
   painel(g,bx,by,bw,bh,14,FUNDO,ouro,3);
   Graphics2D r=(Graphics2D)g.create();r.clip(chanfro(bx+3,by+3,bw-6,bh-6,12));int rx=bx-120+(int)((agora/6)%(bw+240));
   r.setColor(new Color(255,245,200,40));r.fillRect(rx,by,10,bh);r.fillRect(rx+16,by,4,bh);r.dispose();
   trofeu(g,bx+24,by+24,8,ouro,ouroClaro);
   FontePixel.desenhar(g,"CAMPEÃO",bx+130,by+18,2,ouroClaro);
   FontePixel.desenhar(g,"1",bx+130,by+48,7,ouro);
   FontePixel.desenhar(g,p.iniciais,bx+190,by+48,7,ouroClaro);
   String pts=pontos(p.pontos);
   FontePixel.desenhar(g,"PONTOS",bx+bw-28-FontePixel.largura("PONTOS",2),by+18,2,ouroClaro);
   FontePixel.desenhar(g,pts,bx+bw-28-FontePixel.largura(pts,5),by+54,5,ouro);
   // demais: posicao, piloto e pontuacao geral, cada um num botao como os do menu
   FontePixel.desenhar(g,"POS",184,246,2,ROTULO);FontePixel.desenhar(g,"PILOTO",300,246,2,ROTULO);
   FontePixel.desenhar(g,"PONTUAÇÃO GERAL",776-FontePixel.largura("PONTUAÇÃO GERAL",2),246,2,ROTULO);
   for(int i=1;i<lista.size();i++){Entrada e=lista.get(i);int y=268+(i-1)*31;Color c=i==1?prata:i==2?bronze:LETRA;
    painel(g,168,y,624,27,8,FUNDO,i<=2?c.darker():BORDA,2);
    int ty=y+(27-FontePixel.altura(2))/2;
    if(i<=2)trofeu(g,244,y+4,2,c,c.brighter());
    FontePixel.desenhar(g,String.format(Locale.ROOT,"%02d",i+1),184,ty,2,c);
    FontePixel.desenhar(g,e.iniciais,300,ty,2,c);
    String s=pontos(e.pontos);
    FontePixel.desenhar(g,s,776-FontePixel.largura(s,2),ty,2,c);}
  }
  FontePixel.centro(g,rodape,480,550,2,ROTULO);
  g.dispose();
 }
 private static String pontos(int p){return String.format(Locale.ROOT,"%,d",p).replace(',','.');}
 /** Retangulo com os quatro cantos chanfrados, como os botoes do menu. */
 static Polygon chanfro(int x,int y,int w,int h,int c){
  return new Polygon(new int[]{x+c,x+w-c,x+w,x+w,x+w-c,x+c,x,x},new int[]{y,y,y+c,y+h-c,y+h,y+h,y+h-c,y+c},8);
 }
 /** Painel chanfrado: fundo e borda de 'e' px (borda null = so o fundo), com uma linha escura por fora. */
 static void painel(Graphics2D g,int x,int y,int w,int h,int c,Color fundo,Color borda,int e){
  if(borda!=null){g.setColor(new Color(0,2,8));g.fillPolygon(chanfro(x-1,y-1,w+2,h+2,c+1));g.setColor(borda);g.fillPolygon(chanfro(x,y,w,h,c));}
  g.setColor(fundo);g.fillPolygon(borda==null?chanfro(x,y,w,h,c):chanfro(x+e,y+e,w-2*e,h-2*e,Math.max(1,c-e/2)));
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
