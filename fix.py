import re

files = [
    'src/main/java/com/retail/service/impl/InvoiceServiceImpl.java',
    'src/main/java/com/retail/service/impl/RefundServiceImpl.java'
]

for file in files:
    with open(file, 'r', encoding='utf-8') as f:
        content = f.read()

    # Replacements for Stub classes -> Entity classes
    content = content.replace('import com.retail.common.stub.ProductStubRepository;', 'import com.retail.repository.ProductRepository;')
    content = content.replace('import com.retail.common.stub.BranchInventoryStubRepository;', 'import com.retail.repository.BranchInventoryRepository;')
    content = content.replace('import com.retail.common.stub.InventoryTransactionHistoryStubRepository;', 'import com.retail.repository.InventoryTransactionHistoryRepository;')
    
    content = content.replace('import com.retail.common.stub.ProductStub;', 'import com.retail.entity.Product;')
    content = content.replace('import com.retail.common.stub.BranchInventoryStub;', 'import com.retail.entity.BranchInventory;')
    content = content.replace('import com.retail.common.stub.InventoryTransactionHistoryStub;', 'import com.retail.entity.InventoryTransactionHistory;')
    
    # In case import .* was used
    content = content.replace('import com.retail.entity.Invoice;', 'import com.retail.entity.Product;\nimport com.retail.entity.ProductStatus;\nimport com.retail.entity.BranchInventoryId;\nimport com.retail.entity.BranchInventory;\nimport com.retail.entity.InventoryTransactionHistory;\nimport com.retail.entity.Invoice;')
    content = content.replace('import com.retail.entity.Refund;', 'import com.retail.entity.Product;\nimport com.retail.entity.ProductStatus;\nimport com.retail.entity.BranchInventoryId;\nimport com.retail.entity.BranchInventory;\nimport com.retail.entity.InventoryTransactionHistory;\nimport com.retail.entity.Refund;')

    content = content.replace('ProductStubRepository', 'ProductRepository')
    content = content.replace('BranchInventoryStubRepository', 'BranchInventoryRepository')
    content = content.replace('InventoryTransactionHistoryStubRepository', 'InventoryTransactionHistoryRepository')

    content = content.replace('ProductStub', 'Product')
    content = content.replace('BranchInventoryStub', 'BranchInventory')
    content = content.replace('InventoryTransactionHistoryStub', 'InventoryTransactionHistory')

    # Fix ProductStatus enum comparison
    content = re.sub(r'!\s*product\.getStatus\(\)\.equals\("Active"\)', 'product.getStatus() != ProductStatus.Active', content)
    content = re.sub(r'product\.getStatus\(\)\.equals\("Active"\)', 'product.getStatus() == ProductStatus.Active', content)
    content = re.sub(r'"Active"\.equalsIgnoreCase\(pd\.getPromotion\(\)\.getStatus\(\)\)', 'pd.getPromotion().getStatus().equals("Active")', content)

    # Fix entity builders mapping
    # My InvoiceBuilder takes branchId(Integer). The previous regex broke it.
    # ONLY InventoryTransactionHistory uses branch(Branch).
    content = content.replace('InventoryTransactionHistory.builder()', 'InventoryTransactionHistory.builder()\n                    .branch(branchRepository.findById(branchId).orElse(null))\n                    .product(productRepository.findById(detail.getProductId()).orElse(null))\n                    .createdBy(employeeRepository.findById(cashier.getEmployeeId()).orElse(null))')
    
    # Wait, the above would insert duplicates. Let's do it smarter.
    # I will replace the builder calls specifically:
    # In InvoiceServiceImpl:
    content = re.sub(r'\.branchId\(branch\.getBranchId\(\)\)', '.branchId(branch.getBranchId())', content)
    # The only place branchId(branchId) is used on InventoryTransactionHistory is here:
    content = content.replace('.branchId(invoice.getBranchId())', '.branch(branchRepository.findById(invoice.getBranchId()).orElse(null))')
    content = content.replace('.productId(detail.getProductId())', '.product(productRepository.findById(detail.getProductId()).orElse(null))')
    content = content.replace('.createdBy(cashier.getEmployeeId())', '.createdBy(employeeRepository.findById(cashier.getEmployeeId()).orElse(null))')
    
    # For RefundServiceImpl:
    content = content.replace('.branchId(invoice.getBranchId())', '.branch(branchRepository.findById(invoice.getBranchId()).orElse(null))')
    content = content.replace('.productId(rd.getProductId())', '.product(productRepository.findById(rd.getProductId()).orElse(null))')
    content = content.replace('.createdBy(employeeId)', '.createdBy(employeeRepository.findById(employeeId).orElse(null))')

    with open(file, 'w', encoding='utf-8') as f:
        f.write(content)

mappers = [
    'src/main/java/com/retail/mapper/InvoiceMapper.java',
    'src/main/java/com/retail/mapper/RefundMapper.java',
    'src/main/java/com/retail/validator/RefundValidator.java'
]
for file in mappers:
    with open(file, 'r', encoding='utf-8') as f:
        content = f.read()
    content = content.replace('import com.retail.common.stub.ProductStub;', 'import com.retail.entity.Product;')
    content = content.replace('ProductStub', 'Product')
    with open(file, 'w', encoding='utf-8') as f:
        f.write(content)
