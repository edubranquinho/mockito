package br.com.caelum.leilao.servico;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Calendar;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import br.com.caelum.leilao.dominio.Lance;
import br.com.caelum.leilao.dominio.Leilao;
import br.com.caelum.leilao.dominio.Pagamento;
import br.com.caelum.leilao.dominio.Usuario;
import br.com.caelum.leilao.infra.dao.RepositorioDeLeiloes;
import br.com.caelum.leilao.infra.dao.RepositorioDePagamentos;
import br.com.caelum.leilao.infra.relogio.Relogio;

public class GeradorDePagamentoTest {

	@Test
	public void geraPagamentoParaMaiorValor() {
		Leilao leilao = new Leilao("playstation");
		leilao.propoe(new Lance(new Usuario("Eduardo"), 2000));
		leilao.propoe(new Lance(new Usuario("Camilla"), 2500));

		Avaliador avaliador = new Avaliador();
		RepositorioDePagamentos repositorio = mock(RepositorioDePagamentos.class);
		RepositorioDeLeiloes repositorioDeLeiloes = mock(RepositorioDeLeiloes.class);
		when(repositorioDeLeiloes.encerrados()).thenReturn(Arrays.asList(leilao));

		GeradorDePagamento gerador = new GeradorDePagamento(repositorio, repositorioDeLeiloes, avaliador);
		gerador.gera();

		ArgumentCaptor<Pagamento> argumento = ArgumentCaptor.forClass(Pagamento.class);
		verify(repositorio).salvar(argumento.capture());

		Pagamento pagamento = argumento.getValue();
		assertEquals(pagamento.getValor(), 2500, 0.0001);
	}

	@Test
	public void deveEmpurrarParaDiaUtil() {
		Leilao leilao = new Leilao("playstation");
		leilao.propoe(new Lance(new Usuario("Eduardo"), 2000));
		leilao.propoe(new Lance(new Usuario("Camilla"), 2500));

		Avaliador avaliador = new Avaliador();
		RepositorioDePagamentos repositorio = mock(RepositorioDePagamentos.class);
		RepositorioDeLeiloes repositorioDeLeiloes = mock(RepositorioDeLeiloes.class);
		Relogio relogio = mock(Relogio.class);

		Calendar sabado = Calendar.getInstance();
		sabado.set(2017, Calendar.MARCH, 11);

		when(repositorioDeLeiloes.encerrados()).thenReturn(Arrays.asList(leilao));
		when(relogio.hoje()).thenReturn(sabado);

		GeradorDePagamento gerador = new GeradorDePagamento(repositorio, repositorioDeLeiloes, avaliador, relogio);
		gerador.gera();

		ArgumentCaptor<Pagamento> argumento = ArgumentCaptor.forClass(Pagamento.class);
		verify(repositorio).salvar(argumento.capture());

		Pagamento pagamentoGerado = argumento.getValue();

		assertEquals(pagamentoGerado.getData().get(Calendar.DAY_OF_WEEK), Calendar.MONDAY);
		assertEquals(pagamentoGerado.getData().get(Calendar.DAY_OF_MONTH), 13);
	}
}
