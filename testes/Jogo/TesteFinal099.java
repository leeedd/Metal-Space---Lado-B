package Jogo;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Method;
import javax.imageio.ImageIO;
/** Luta final: Bardo (1o dente), Monge (2o dente), Belzebu invocado, Zaref com a corrente do Demonio das Correntes, e os creditos. */
public final class TesteFinal099 {
 static int n;
 static void ok(boolean b,String m){if(!b)throw new AssertionError(m);n++;}
 static Object get(Object o,String c)throws Exception{return TesteInvasao093.get(o,c);}
 static void call(Fase f,String m)throws Exception{Method x=Fase.class.getDeclaredMethod(m);x.setAccessible(true);x.invoke(f);}
 static String pasta;
 static void foto(Fase f,String nome)throws Exception{if(pasta==null)return;f.setSize(960,600);BufferedImage b=new BufferedImage(960,600,BufferedImage.TYPE_INT_RGB);Graphics2D g=b.createGraphics();f.paint(g);g.dispose();ImageIO.write(b,"png",new File(pasta,nome+".png"));}
 /** Simula com a nave sempre inteira (o teste e da luta, nao do piloto). */
 static void tick(Fase f,double s){for(int i=0;i<(int)(s*60);i++){f.getNave().curar(1000);f.atualizarSimulacao(1.0/60);}}
 static void bater(Fase f,int ato)throws Exception{Chefe c=f.getChefeTeste();for(int i=0;i<500&&c.getAtoFinalTeste()==ato;i++){if(c.receberDano(25)){call(f,"derrotarChefao");return;}tick(f,.02);}}
 public static void main(String[] a)throws Exception{
  pasta=a.length>0?a[0]:null;if(pasta!=null)new File(pasta).mkdirs();
  Fase f=TesteInvasao093.fase(100);TesteInvasao093.set(f,"chefesDerrotados",8);TesteInvasao093.set(f,"van_infernoVencido",true);TesteInvasao093.set(f,"reuniaoVencida",true);call(f,"iniciarFinal");
  Chefe c=f.getChefeTeste();
  ok(c!=null&&c.getFase()==Fase.FINAL,"luta final comeca");ok(c.getAtoFinalTeste()==ChefeFinal.BARDO&&c.getDentesTeste()==0,"primeiro o Bardo");
  ok(!c.receberDano(50)&&c.getVida()==c.getVidaMaxima(),"entrando nao leva dano");
  tick(f,5.8);foto(f,"final_1_bardo");ok(!f.getTirosChefesTeste().isEmpty(),"Bardo ataca");
  bater(f,ChefeFinal.BARDO);ok(c.getAtoFinalTeste()==ChefeFinal.MONGE&&c.getDentesTeste()==0,"no primeiro ato as almas voltam aos ossos");
  tick(f,.1);ok(f.getTirosChefesTeste().isEmpty(),"troca de ato limpa os tiros");
  tick(f,3.6);foto(f,"final_2_monge");
  bater(f,ChefeFinal.MONGE);ok(c.getAtoFinalTeste()==ChefeFinal.BELZEBU&&c.getDentesTeste()==2,"as duas almas se fundem");
  tick(f,1.0);foto(f,"final_3a_almas");tick(f,1.3);foto(f,"final_3b_fusao");tick(f,2.0);foto(f,"final_3c_buraco_negro");tick(f,2.4);foto(f,"final_3d_engolido");tick(f,1.2);foto(f,"final_3e_dobra");tick(f,1.6);foto(f,"final_3f_espiral");tick(f,1.4);foto(f,"final_3g_inferno");tick(f,1.5);ok(c.receberDano(0)==false&&c.getNome().startsWith("RIO"),"rio de lava antes do Belzebu");tick(f,6);foto(f,"final_3h_rio");tick(f,10);foto(f,"final_3i_horda");tick(f,12);foto(f,"final_3j_bateria");tick(f,10);foto(f,"final_3k_chegando");tick(f,3.5);foto(f,"final_4_belzebu");
  ok(c.getVida()==c.getVidaMaxima(),"Belzebu com a vida cheia");
  boolean[] viu=new boolean[3];String[] nm={"final_4b_bateria","final_4c_guitarra","final_4d_canto"};Object fimB=get(c,"fim");
  for(int i=0;i<60*26&&!(viu[0]&&viu[1]&&viu[2]);i++){f.getNave().curar(1000);f.getNave().teleportar(120,300);f.atualizarSimulacao(1.0/60);int m=(int)get(fimB,"modoB");double q=(double)get(fimB,"quadroB");
   if(!viu[m]&&(m==0&&q==3||m==1&&q==2||m==2&&q==2)){viu[m]=true;foto(f,nm[m]);}}
  ok(viu[0]&&viu[1]&&viu[2],"Belzebu alterna bateria, guitarra e canto");
  bater(f,ChefeFinal.BELZEBU);ok(c.getAtoFinalTeste()==ChefeFinal.ZAREF&&c.estaVivo(),"Belzebu vira o Zaref");
  tick(f,1.6);foto(f,"final_5_revelacao");tick(f,3);
  boolean raio=false,espada=false;
  for(int i=0;i<60*24&&!(raio&&espada);i++){
   f.getNave().curar(1000);f.getNave().teleportar(120,300);f.atualizarSimulacao(1.0/60);Object fim=get(c,"fim");
   if(!raio&&(double)get(fim,"raio")>0){raio=true;foto(f,"final_6_raio_arco_iris");}
   if(!espada&&(double)get(fim,"investida")>0&&(double)get(fim,"investida")<.5){espada=true;foto(f,"final_7_zaref_espada");}
  }
  ok(raio,"Zaref solta o arco-iris");ok(espada,"Zaref avanca de espada");
  bater(f,ChefeFinal.ZAREF);ok(c.getAtoFinalTeste()==ChefeFinal.CORRENTES&&c.estaVivo(),"Zaref vira o demonio do Demonio das Correntes");
  tick(f,1.8);foto(f,"final_8_vira_demonio_correntes");tick(f,3.2);
  boolean agarrou=false,chicote=false,mergulho=false;double x0=0;
  for(int i=0;i<60*30&&!(agarrou&&chicote&&mergulho);i++){
   f.getNave().curar(1000);f.getNave().teleportar(120,300);f.atualizarSimulacao(1.0/60);Object fim=get(c,"fim");
   if(!chicote&&(double)get(fim,"chicote")>0&&(double)get(fim,"chicote")<.4){chicote=true;foto(f,"final_9_chicote");}
   if(!mergulho&&(double)get(fim,"demonio")>0&&(double)get(fim,"demonioX")<600){mergulho=true;foto(f,"final_10_mergulho");}
   if(!agarrou&&c.naveAgarrada()){agarrou=true;x0=f.getNave().getX();for(int k=0;k<20;k++){f.getNave().curar(1000);f.atualizarSimulacao(1.0/60);}foto(f,"final_11_corrente");ok(f.getNave().getX()>x0,"corrente arrasta a nave");}
  }
  ok(chicote,"chicote da corrente");ok(mergulho,"mergulho do demonio");ok(agarrou,"corrente do Demonio das Correntes agarra a nave");
  bater(f,ChefeFinal.CORRENTES);ok(f.getChefeTeste()==null,"demonio derrotado");ok((boolean)get(f,"finalVencido"),"final vencido");
  for(int i=0;i<60*20&&!f.getEtapaTeste().equals("CREDITOS");i++){f.getNave().curar(1000);f.atualizarSimulacao(1.0/60);}
  ok(f.getEtapaTeste().equals("CREDITOS"),"depois do Demonio das Correntes vem os creditos");
  System.out.println("OK Final: "+n+" verificacoes");System.exit(0);
 }
}
