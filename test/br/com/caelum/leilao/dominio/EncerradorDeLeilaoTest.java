package br.com.caelum.leilao.dominio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.*;

import br.com.caelum.leilao.builder.CriadorDeLeilao;
import br.com.caelum.leilao.infra.dao.LeilaoDao;
import br.com.caelum.leilao.infra.dao.RepositorioDeLeiloes;
import br.com.caelum.leilao.infra.email.EnviadorDeEmail;
import br.com.caelum.leilao.servico.EncerradorDeLeilao;

public class EncerradorDeLeilaoTest {

	private EnviadorDeEmail carteiro;
	private Calendar dataAntiga;

	@Before
	public void setUp() {
		carteiro = mock(EnviadorDeEmail.class);
		dataAntiga = Calendar.getInstance();
		dataAntiga.set(1999, 1, 20);
	}

	@Test
	public void deveEncerrarLeiloesQueComecaramAUmaSemana() {
		Leilao leilao = new CriadorDeLeilao().para("TV de plasma").naData(dataAntiga).constroi();
		Leilao leilao2 = new CriadorDeLeilao().para("Geladeira").naData(dataAntiga).constroi();

		List<Leilao> leiloesAntigos = Arrays.asList(leilao, leilao2);

		RepositorioDeLeiloes daoFalso = mock(LeilaoDao.class);

		when(daoFalso.correntes()).thenReturn(leiloesAntigos);

		EncerradorDeLeilao encerrador = new EncerradorDeLeilao(daoFalso, carteiro);
		encerrador.encerra();

		assertEquals(2, encerrador.getTotalEncerrados());
		assertTrue(leilao.isEncerrado());
		assertTrue(leilao2.isEncerrado());
	}

	@Test
	public void deveEnviarEmail() {
		Leilao leilao = new CriadorDeLeilao().para("TV de plasma").naData(dataAntiga).constroi();

		List<Leilao> leiloesAntigos = Arrays.asList(leilao);

		RepositorioDeLeiloes daoFalso = mock(LeilaoDao.class);

		when(daoFalso.correntes()).thenReturn(leiloesAntigos);
		doNothing().when(carteiro).envia(leilao);

		EncerradorDeLeilao encerrador = new EncerradorDeLeilao(daoFalso, carteiro);
		encerrador.encerra();

		InOrder inOrder = Mockito.inOrder(daoFalso, carteiro);
		inOrder.verify(daoFalso, times(1)).atualiza(leilao);
		inOrder.verify(carteiro, times(1)).envia(leilao);
	}

	@Test
	public void naoDeveEncerrarLeiloesDeOntem() {
		Calendar ontem = Calendar.getInstance();
		ontem.add(Calendar.DAY_OF_YEAR, -1);

		Leilao leilao = new CriadorDeLeilao().para("TV de plasma").naData(ontem).constroi();
		List<Leilao> leiloesDeOntem = Arrays.asList(leilao);

		RepositorioDeLeiloes daoFalso = mock(LeilaoDao.class);

		when(daoFalso.correntes()).thenReturn(leiloesDeOntem);

		EncerradorDeLeilao encerrador = new EncerradorDeLeilao(daoFalso, carteiro);
		encerrador.encerra();

		assertEquals(0, encerrador.getTotalEncerrados());
		assertFalse(leilao.isEncerrado());

	}

	@Test
	public void naoDeveFazerNada() {
		LeilaoDao daoFalso = mock(LeilaoDao.class);

		when(daoFalso.correntes()).thenReturn(new ArrayList<Leilao>());
		EncerradorDeLeilao encerrador = new EncerradorDeLeilao(daoFalso, carteiro);
		encerrador.encerra();

		assertEquals(0, encerrador.getTotalEncerrados());

	}

	@Test
	public void deveAtualizarLeiloesEncerrados() {
		Leilao leilao = new CriadorDeLeilao().para("TV de plasma").naData(dataAntiga).constroi();
		List<Leilao> leiloesAntigos = Arrays.asList(leilao);
		RepositorioDeLeiloes daoFalso = mock(LeilaoDao.class);

		when(daoFalso.correntes()).thenReturn(leiloesAntigos);
		EncerradorDeLeilao encerrador = new EncerradorDeLeilao(daoFalso, carteiro);
		encerrador.encerra();

		verify(daoFalso, times(1)).atualiza(leilao);
	}

	@Test
	public void naoDeveEncerrarLeiloesQueComecaramMenosDeUmaSemanaAtras() {
		Calendar ontem = Calendar.getInstance();
		ontem.add(Calendar.DAY_OF_MONTH, -1);

		Leilao leilao1 = new CriadorDeLeilao().para("TV de plasma").naData(ontem).constroi();
		Leilao leilao2 = new CriadorDeLeilao().para("Geladeira").naData(ontem).constroi();

		RepositorioDeLeiloes daoFalso = mock(LeilaoDao.class);
		when(daoFalso.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));

		EncerradorDeLeilao encerrador = new EncerradorDeLeilao(daoFalso, carteiro);
		encerrador.encerra();

		assertEquals(0, encerrador.getTotalEncerrados());
		assertFalse(leilao1.isEncerrado());
		assertFalse(leilao2.isEncerrado());

		verify(daoFalso, never()).atualiza(leilao1);
	}

	@Test
	public void deveContinuarAExecucaoMesmoQuandoDaoFalha() {
		Leilao leilao1 = new CriadorDeLeilao().para("TV de plasma").naData(dataAntiga).constroi();
		Leilao leilao2 = new CriadorDeLeilao().para("Geladeira").naData(dataAntiga).constroi();
		RepositorioDeLeiloes daoFalso = mock(LeilaoDao.class);
		when(daoFalso.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));
		doThrow(new RuntimeException()).when(daoFalso).atualiza(leilao1);

		EncerradorDeLeilao encerrador = new EncerradorDeLeilao(daoFalso, carteiro);
		encerrador.encerra();

		verify(daoFalso).atualiza(leilao2);
		verify(carteiro).envia(leilao2);
		verify(carteiro, never()).envia(leilao1);
	}

	@Test
	public void deveContinuarAExecucaoMesmoQuandoOEnvioDeEmailFalha() {
		Leilao leilao1 = new CriadorDeLeilao().para("TV de plasma").naData(dataAntiga).constroi();
		Leilao leilao2 = new CriadorDeLeilao().para("Geladeira").naData(dataAntiga).constroi();
		RepositorioDeLeiloes daoFalso = mock(LeilaoDao.class);
		when(daoFalso.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));
		doThrow(new RuntimeException()).when(carteiro).envia(leilao1);

		EncerradorDeLeilao encerrador = new EncerradorDeLeilao(daoFalso, carteiro);
		encerrador.encerra();
		
		verify(daoFalso).atualiza(leilao1);
		verify(daoFalso).atualiza(leilao2);
		verify(carteiro).envia(leilao2);
	}
	
	@Test
	public void naoDeveEnviarEmailSeAtualizarLeilaoLancarException(){
		
		Leilao leilao1 = new CriadorDeLeilao().para("TV de plasma").naData(dataAntiga).constroi();
		Leilao leilao2 = new CriadorDeLeilao().para("Geladeira").naData(dataAntiga).constroi();
		RepositorioDeLeiloes daoFalso = mock(LeilaoDao.class);
		when(daoFalso.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));
		doThrow(new RuntimeException()).when(daoFalso).atualiza(any(Leilao.class));
		
		EncerradorDeLeilao encerrador = new EncerradorDeLeilao(daoFalso, carteiro);
		encerrador.encerra();
		
		verify(carteiro, never()).envia(any(Leilao.class));
	}

}
