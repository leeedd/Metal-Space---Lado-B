package Jogo;
import java.awt.event.KeyEvent;import java.lang.reflect.Method;
/** Fuga do buraco negro: entra depois do ultimo chefe, o jogador controla a potencia, atira e se fere, e ao chegar na saida vem os creditos. */
public final class TesteFuga099 {
 static int n;static void ok(boolean b,String m){if(!b)throw new AssertionError(m);n++;}
 static void tick(Fase f,double s){for(int i=0;i<(int)(s*60);i++)f.atualizarSimulacao(1.0/60);}
 public static void main(String[] a)throws Exception{
  Fase f=TesteInvasao093.fase(100);TesteInvasao093.set(f,"chefesDerrotados",8);TesteInvasao093.set(f,"van_infernoVencido",true);TesteInvasao093.set(f,"reuniaoVencida",true);TesteInvasao093.set(f,"finalVencido",true);
  java.lang.reflect.Field fe=Fase.class.getDeclaredField("etapa");fe.setAccessible(true);fe.set(f,Enum.valueOf((Class)fe.getType(),"INTERLUDIO"));TesteInvasao093.set(f,"tempoEtapa",0.0);TesteInvasao093.set(f,"trofeuPendente",0);
  Method vp=Fase.class.getDeclaredMethod("atualizarTransicao",double.class);vp.setAccessible(true);vp.invoke(f,.02);
  FugaFinal u=f.getFugaTeste();ok(u!=null,"depois do ultimo chefe comeca a fuga");
  tick(f,FugaFinal.INTRO+.2);ok(u.intro==0,"a camera entra na cabine");
  // aceleracao livre: com a alavanca no maximo anda mais rapido que no minimo
  u.tecla(KeyEvent.VK_S,true);tick(f,3);u.tecla(KeyEvent.VK_S,false);double lenta=u.vel;ok(u.potencia==0,"alavanca vai a zero");
  u.tecla(KeyEvent.VK_W,true);tick(f,4);u.tecla(KeyEvent.VK_W,false);ok(u.vel>lenta*3,"acelera quando o jogador quer: "+lenta+" -> "+u.vel);
  // armas
  int mun=u.municao;u.tecla(KeyEvent.VK_SPACE,true);tick(f,.5);u.tecla(KeyEvent.VK_SPACE,false);ok(u.municao<mun,"canhoes gastam municao");
  int mi=u.misseis;u.tecla(KeyEvent.VK_F,true);u.tecla(KeyEvent.VK_F,false);ok(u.misseis==mi-1,"missil");
  int bo=u.bombas;u.tecla(KeyEvent.VK_B,true);u.tecla(KeyEvent.VK_B,false);ok(u.bombas==bo-1,"bomba");
  // dano: uma rocha na cara da nave fere estrutura ou escudo
  double esc=u.escudo,est=u.sist[FugaFinal.ESTRUTURA];FugaPista.Obj o=new FugaPista.Obj();o.tipo=FugaPista.ROCHA;o.x=u.sx;o.y=u.sy;o.z=1.2;o.r=.3;o.vida=3;u.pista.objs.add(o);tick(f,.2);
  ok(u.escudo<esc||u.sist[FugaFinal.ESTRUTURA]<est,"colisao fere a nave");
  // motor ferido limita a potencia
  u.sist[FugaFinal.MOTORES]=20;ok(u.potenciaMax()<.6,"motor ferido nao chega na potencia toda");u.sist[FugaFinal.MOTORES]=100;
  // ate a saida (invulneravel no teste, com potencia alta)
  u.tecla(KeyEvent.VK_W,true);for(int i=0;i<60*240&&f.getFugaTeste()!=null;i++){for(int s=0;s<6;s++)u.sist[s]=100;u.escudo=100;f.atualizarSimulacao(1.0/60);}
  ok(f.getFugaTeste()==null,"chega na saida");ok(f.getEtapaTeste().equals("CREDITOS"),"depois da fuga vem os creditos: "+f.getEtapaTeste());
  System.out.println("OK Fuga: "+n+" verificacoes");
 }
}
