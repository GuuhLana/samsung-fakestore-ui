package bean;

import java.io.Serializable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import dto.CartDTO;
import dto.CartDTO.ProductQuantity;
import dto.ProductDTO;
import dto.UserDTO;

@ManagedBean
@SessionScoped
public class PedidoBean implements Serializable {

	private static final long serialVersionUID = 1L;

	private List<UserDTO> clientes;
	private List<CartDTO> pedidos;
	private List<ProductDTO> produtos;
	private Long clienteSelecionado;
	private String numeroPedido;
	private Date dataInicio;
	private Date dataFim;
	private boolean pesquisaRealizada;

	private final ObjectMapper mapper = new ObjectMapper();

	@PostConstruct
	public void init() {
		buscarClientes();
		buscarProdutos();
	}

	public void buscarClientes() {
		try {
			HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8081/api/users")).GET().build();
			HttpResponse<String> res = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
			clientes = mapper.readValue(res.body(), new TypeReference<List<UserDTO>>() {
			});
		} catch (Exception e) {
			e.printStackTrace();
			clientes = Collections.emptyList();
		}
	}

	public void buscarProdutos() {
		try {
			HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8081/api/products")).GET()
					.build();
			HttpResponse<String> res = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
			produtos = mapper.readValue(res.body(), new TypeReference<List<ProductDTO>>() {
			});
		} catch (Exception e) {
			e.printStackTrace();
			produtos = Collections.emptyList();
		}
	}

	public void buscarPedidos() {
		if ((clienteSelecionado == null || clienteSelecionado == 0L) && (numeroPedido == null || numeroPedido.isBlank())
				&& dataInicio == null && dataFim == null) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_WARN, "Enter at least one filter", null));
			pedidos = Collections.emptyList();
			pesquisaRealizada = true;
			return;
		}

		try {
			HttpRequest req = HttpRequest.newBuilder().uri(URI.create("http://localhost:8081/api/carts")).GET().build();
			HttpResponse<String> res = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
			List<CartDTO> todos = mapper.readValue(res.body(), new TypeReference<List<CartDTO>>() {
			});

			Map<Long, Double> precoPorId = produtos.stream()
					.collect(Collectors.toMap(ProductDTO::getId, ProductDTO::getPrice));

			DateTimeFormatter isoFormatter = DateTimeFormatter.ISO_DATE_TIME;

			List<CartDTO> filtrados = todos.stream()

					.filter(p -> clienteSelecionado == null || Objects.equals(p.getUserId(), clienteSelecionado))
					.filter(p -> numeroPedido == null || numeroPedido.isBlank()
							|| String.valueOf(p.getId()).equals(numeroPedido.trim()))
					.filter(p -> {
						if (dataInicio == null && dataFim == null)
							return true;
						try {
							LocalDate dataPedido = LocalDate.parse(p.getDate(), isoFormatter);
							LocalDate inicio = dataInicio != null ? convertToLocalDate(dataInicio) : null;
							LocalDate fim = dataFim != null ? convertToLocalDate(dataFim) : null;
							return (inicio == null || !dataPedido.isBefore(inicio))
									&& (fim == null || !dataPedido.isAfter(fim));
						} catch (Exception e) {
							return false;
						}
					})

					.peek(p -> {
						double total = 0;
						for (ProductQuantity prod : p.getProducts()) {
							ProductDTO detalhes = produtos.stream()
									.filter(produto -> Objects.equals(produto.getId(), prod.getProductId())).findFirst()
									.orElse(null);
							if (detalhes != null) {
								prod.setPrice(detalhes.getPrice());
								prod.setTitle(detalhes.getTitle());
								prod.setDescription(detalhes.getDescription());
								prod.setImage(detalhes.getImage());
								total += detalhes.getPrice() * prod.getQuantity();
							}
						}

						UserDTO cliente = clientes.stream().filter(c -> Objects.equals(c.getId(), p.getUserId()))
								.findFirst().orElse(null);
						if (cliente != null) {
							p.setUsername(cliente.getUsername());
						}
					})

					.collect(Collectors.toList());

			this.pedidos = filtrados;
		} catch (Exception e) {
			e.printStackTrace();
			pedidos = Collections.emptyList();
		}

		pesquisaRealizada = true;
	}

	private LocalDate convertToLocalDate(Date date) {
		return date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
	}

	public void resetarFiltros() {
		clienteSelecionado = null;
		numeroPedido = null;
		dataInicio = null;
		dataFim = null;
		pedidos = null;
		pesquisaRealizada = false;
	}

	public List<UserDTO> getClientes() {
		return clientes;
	}

	public void setClientes(List<UserDTO> clientes) {
		this.clientes = clientes;
	}

	public List<CartDTO> getPedidos() {
		return pedidos;
	}

	public void setPedidos(List<CartDTO> pedidos) {
		this.pedidos = pedidos;
	}

	public Long getClienteSelecionado() {
		return clienteSelecionado;
	}

	public void setClienteSelecionado(Long clienteSelecionado) {
		this.clienteSelecionado = clienteSelecionado;
	}

	public String getNumeroPedido() {
		return numeroPedido;
	}

	public void setNumeroPedido(String numeroPedido) {
		this.numeroPedido = numeroPedido;
	}

	public Date getDataInicio() {
		return dataInicio;
	}

	public void setDataInicio(Date dataInicio) {
		this.dataInicio = dataInicio;
	}

	public Date getDataFim() {
		return dataFim;
	}

	public void setDataFim(Date dataFim) {
		this.dataFim = dataFim;
	}

	public boolean isPesquisaRealizada() {
		return pesquisaRealizada;
	}

	public void setPesquisaRealizada(boolean pesquisaRealizada) {
		this.pesquisaRealizada = pesquisaRealizada;
	}
}
